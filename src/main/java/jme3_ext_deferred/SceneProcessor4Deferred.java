package jme3_ext_deferred;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import rx.Observable;
import rx.subjects.BehaviorSubject;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.post.SceneProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.NullComparator;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.FrameBuffer;

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
 * @TODO refactor debugMaterial Management
 * @author David Bernard
 */
//http://www.txutxi.com/?p=182
//http://www.txutxi.com/?p=83
@RequiredArgsConstructor
public class SceneProcessor4Deferred implements SceneProcessor {
	Pass4GBuffer pass4gbuffer;
	Pass4AO pass4ao;
	Pass4Tex pass4tex;
	Pass4LBuffer pass4lbuffer;

	private BehaviorSubject<SceneProcessor4Deferred> onChange0 = BehaviorSubject.create();
	public Observable<SceneProcessor4Deferred> onChange = onChange0;
	public final AssetManager assetManager;
	private RenderManager rm;
	public ViewPort vp;
	private float tpf;
	private Geometry finalQuad;
	public final GeometryList lights = new GeometryList(new NullComparator());
	private List<Material> mats = new LinkedList<>();

	public void initialize(RenderManager rm, ViewPort vp) {
		this.rm = rm;
		this.vp = vp;
		onChange.subscribe(this::materialsUpdateTextures);

		finalQuad = new Geometry("finalQuad", new Quad(1, 1));
		finalQuad.setCullHint(Spatial.CullHint.Never);
		finalQuad.setQueueBucket(Bucket.Opaque);

		reshape(vp, vp.getCamera().getWidth(), vp.getCamera().getHeight());
	}

	public Material getDebugMaterial(DebugMaterialKey key) {
		Material m = new Material(assetManager, "MatDefs/deferred/debug_gbuffer.j3md");
		m.getAdditionalRenderState().setDepthTest(false);
		m.getAdditionalRenderState().setDepthWrite(false);
		m.setBoolean("FullView", false);
		//m.selectTechnique("objgreen", rm);
		m.selectTechnique(key.name(), rm);
		mats.add(m);
		materialsUpdateTextures(this);
		return m;
	}

	Vector3f m_FrustumCorner = new Vector3f();
	Matrix4f m_ViewProjectionMatrixInverse = new Matrix4f();
	void updateMaterials() {
		Camera camera = vp.getCamera();
		//camera.getViewProjectionMatrix().invert(m_ViewProjectionMatrixInverse);
		//m_FrustumNearFar.set(camera.getFrustumNear(), camera.getFrustumFar());
		float farY = (camera.getFrustumTop() / camera.getFrustumNear()) * camera.getFrustumFar();
		float farX = farY * ((float) camera.getWidth() / (float) camera.getHeight());
		m_FrustumCorner.set(farX, farY, vp.getCamera().getFrustumFar());
		for(Material m : mats) {
			m.setMatrix4("ViewProjectionMatrixInverse", m_ViewProjectionMatrixInverse);
			//m.setVector2("FrustumNearFar", m_FrustumNearFar);
			m.setVector3("FrustumCorner", m_FrustumCorner);
		}
	}

	void materialsUpdateTextures(SceneProcessor4Deferred buffers) {
		for(Material m : mats) {
			Set<String> params = m.getMaterialDef().getMaterialParams().stream().map((mp) -> mp.getName()).collect(Collectors.toSet());
			if (params.contains("DiffuseBuffer")) m.setTexture("DiffuseBuffer", buffers.pass4gbuffer.gbuffer.diffuse);
			if (params.contains("SpecularBuffer")) m.setTexture("SpecularBuffer", buffers.pass4gbuffer.gbuffer.specular);
			if (params.contains("NormalBuffer")) m.setTexture("NormalBuffer", buffers.pass4gbuffer.gbuffer.normal);
			if (params.contains("DepthBuffer")) m.setTexture("DepthBuffer", buffers.pass4gbuffer.gbuffer.depth);
			if (params.contains("AOBuffer")) m.setTexture("AOBuffer", buffers.pass4ao.aobuffer.tex);
		}
	}

	public void reshape(ViewPort vp, int w, int h) {
		cleanup();
		pass4gbuffer = new Pass4GBuffer(w, h, vp, rm);
		pass4ao = new Pass4AO(w, h, vp, rm, assetManager, pass4gbuffer.gbuffer, finalQuad, false);
		pass4lbuffer = new Pass4LBuffer(w, h, vp, rm, assetManager, lights, pass4gbuffer.gbuffer);
		//pass4tex = new Pass4Tex(finalQuad, vp, rm, assetManager, pass4ao.finalTex);
		pass4tex = new Pass4Tex(finalQuad, vp, rm, assetManager, pass4lbuffer.lbuffer.tex);
		onChange0.onNext(this);
	}

	public boolean isInitialized() {
		return pass4gbuffer != null;
	}

	public void preFrame(float tpf) {
		this.tpf = tpf;
		finalQuad.updateLogicalState(tpf);
		finalQuad.updateGeometricState();
		pass4lbuffer.update(tpf);
	}

	public void postQueue(RenderQueue rq) {
		String techOrig = rm.getForcedTechnique();
		updateMaterials();
		pass4gbuffer.render();
		pass4ao.render();
		pass4lbuffer.render();
		pass4tex.render();
		rm.setForcedTechnique(techOrig);
	}

	public void postFrame(FrameBuffer out) {
		//rm.getRenderer().setFrameBuffer(out);
	}

	public void cleanup() {
		if (pass4gbuffer != null) {
			pass4gbuffer.dispose();
			pass4gbuffer = null;
		}
		if (pass4ao != null) {
			pass4ao.dispose();
			pass4ao = null;
		}
		if (pass4lbuffer != null) {
			pass4lbuffer.dispose();
			pass4lbuffer = null;
		}
		if (pass4tex != null) {
			pass4tex.dispose();
			pass4tex = null;
		}
	}

}

