package jme3_ext_deferred

import rx.Observable
import rx.subjects.BehaviorSubject
import com.jme3.asset.AssetManager
import com.jme3.post.SceneProcessor
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.renderer.queue.RenderQueue
import com.jme3.scene.Geometry
import com.jme3.scene.Spatial
import com.jme3.scene.shape.Quad
import com.jme3.texture.FrameBuffer
import org.eclipse.xtend.lib.annotations.FinalFieldsConstructor

/** 
 * @see http://www.cescg.org/CESCG-2012/papers/Ferko-Real-time_Lighting_Effects_using_Deferred_Shading.pdf
 * @see http://ogldev.atspace.co.uk/www/tutorial36/tutorial36.html
 * @see https://github.com/kwando/dmonkey/blob/master/src/me/merciless/dmonkey/DeferredSceneProcessor.java
 * @TODO support postprocessing filter (FXAA, bloom, glow,...)
 * @TODO use lights of the scene (rootNode) not the light on display quad
 * @TODO cast-received shadow (hybrid map + volume)
 * @TODO create a deep gbuffer
 * @TODO Motion blur using gbuffer
 * @TODO pseudo-Radiosity using deep-gbuffer
 * @author David Bernard
 */
//http://www.txutxi.com/?p=182
//http://www.txutxi.com/?p=83
@FinalFieldsConstructor
class SceneProcessor4Deferred implements SceneProcessor {
	package Pass4GBuffer pass4gbuffer
	//package Pass4AO_mssao pass4ao
	package Pass4AO pass4ao
	package Pass4Tex pass4tex
	package Pass4LBuffer pass4lbuffer
	package Pass4Shade pass4shade
	BehaviorSubject<SceneProcessor4Deferred> onChange0 = BehaviorSubject::create()
	public Observable<SceneProcessor4Deferred> onChange = onChange0
	public final AssetManager assetManager
	public final MatIdManager matIdManager
	package RenderManager rm
	package ViewPort vp
	Geometry finalQuad

	override void initialize(RenderManager rm, ViewPort vp) {
		this.rm = rm
		this.vp = vp
		finalQuad = new Geometry("finalQuad", new Quad(1, 1))
		finalQuad.setCullHint(Spatial::CullHint::Never) // finalQuad.setQueueBucket(Bucket.Opaque);
		finalQuad.setMaterial(null)
		reshape(vp, vp.getCamera().getWidth(), vp.getCamera().getHeight())
	}

	override void reshape(ViewPort vp, int w, int h) {
		cleanup()
		pass4gbuffer = new Pass4GBuffer(w, h, vp, rm)
		pass4ao = new Pass4AO(w, h, vp, rm, assetManager, pass4gbuffer.gbuffer, finalQuad, false)
		//pass4ao = new Pass4AO_mssao(w, h, vp, rm, assetManager, pass4gbuffer.gbuffer, 5)
		pass4lbuffer = new Pass4LBuffer(w, h, vp, rm, assetManager, pass4gbuffer.gbuffer, matIdManager.tableTex) // pass4tex = new Pass4Tex(finalQuad, vp, rm, assetManager, pass4ao.finalTex);
		pass4shade = new Pass4Shade(finalQuad, vp, rm, assetManager, pass4gbuffer.gbuffer, matIdManager.tableTex, pass4ao.finalTex, pass4lbuffer.lbuffer.tex)
		pass4tex = new Pass4Tex(finalQuad, vp, rm, assetManager, pass4ao.finalTex)
		onChange0.onNext(this)
	}

	override boolean isInitialized() {
		return pass4gbuffer !== null
	}

	override void preFrame(float tpf) {
		finalQuad.updateLogicalState(tpf)
		finalQuad.updateGeometricState()
	}

	override void postQueue(RenderQueue rq) {
		rm.getRenderer().clearBuffers(true, true, true)
		val techOrig = rm.getForcedTechnique()
		pass4gbuffer.render()
		pass4ao.render()
		pass4lbuffer.render(rq)
		pass4shade.render()
		//pass4tex.render()
		rm.setForcedTechnique(techOrig)
	}

	override void postFrame(FrameBuffer out) {
		// rm.getRenderer().setFrameBuffer(out);
	}

	override void cleanup() {
		if (pass4gbuffer !== null) {
			pass4gbuffer.dispose()
			pass4gbuffer = null
		}
		if (pass4ao !== null) {
			pass4ao.dispose()
			pass4ao = null
		}
		if (pass4lbuffer !== null) {
			pass4lbuffer.dispose()
			pass4lbuffer = null
		}
		if (pass4tex !== null) {
			pass4tex.dispose()
			pass4tex = null
		}

	}

}
