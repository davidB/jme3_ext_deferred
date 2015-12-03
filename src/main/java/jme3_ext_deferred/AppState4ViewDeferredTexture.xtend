package jme3_ext_deferred

import java.util.LinkedList
import java.util.List
import rx.Subscription
import com.jme3.app.Application
import com.jme3.app.SimpleApplication
import com.jme3.app.state.AbstractAppState
import com.jme3.app.state.AppStateManager
import com.jme3.material.Material
import com.jme3.math.Matrix4f
import com.jme3.math.Vector2f
import com.jme3.math.Vector3f
import com.jme3.math.Vector4f
import com.jme3.renderer.Camera
import com.jme3.renderer.RenderManager
import com.jme3.scene.Node
import com.jme3.ui.Picture

class AppState4ViewDeferredTexture extends AbstractAppState {
	public enum ViewKey {
		normals,
		depths,
		albedos,
		speculars,
		matIds,
		mats,
		ao,
		positions,
		lights,
		texture
	} // ,stencil // doesn't work may require investigation / extension like https://www.opengl.org/registry/specs/ARB/stencil_texturing.txt

	final package SceneProcessor4Deferred sp4d
	final package ViewKey[] keys
	final package Picture[] display
	Subscription sub
	List<Material> mats = new LinkedList()

	new(SceneProcessor4Deferred sp4d, ViewKey... keys) {
		this.sp4d = sp4d
		this.keys = keys
		this.display = newArrayOfSize(keys.length)
		sp4d.onChange.subscribe([materialsUpdate])
	}

	def package Material getDebugMaterial(ViewKey key) {
		var Material m = new Material(sp4d.assetManager, "MatDefs/debug/debug_gbuffer.j3md")
		m.getAdditionalRenderState().setDepthTest(false)
		m.getAdditionalRenderState().setDepthWrite(false)
		m.setBoolean("FullView", false) // m.selectTechnique("objgreen", sp4d.rm);
		// m.selectTechnique("fullgreen", sp4d.rm);
		m.selectTechnique(key.name(), sp4d.rm)
		mats.add(m)
		materialsUpdate(sp4d)
		return m
	}

	def package void materialsUpdate(SceneProcessor4Deferred sp) {
		for (Material m : mats) {
			// Set<String> params = m.getMaterialDef().getMaterialParams().stream().map((mp) -> mp.getName()).collect(Collectors.toSet());
			m.setTexture("NormalBuffer", sp.pass4gbuffer.gbuffer.normal)
			m.setTexture("DepthBuffer", sp.pass4gbuffer.gbuffer.depth)
			m.setTexture("AOBuffer", sp.pass4ao.finalTex)
			m.setTexture("MatBuffer", sp.matIdManager.tableTex)
			m.setTexture("AlbedoBuffer", sp.pass4gbuffer.gbuffer.albedo)
			m.setTexture("SpecularBuffer", sp.pass4gbuffer.gbuffer.specular)
			m.setTexture("LBuffer", sp.pass4lbuffer.lbuffer.tex)
			m.setTexture("Texture", sp.pass4lbuffer.ligthPeerTmpl4SpotAndShadow.shadowMapGen.shadowMap0) // m.setTexture("Texture", sp.pass4gbuffer.gbuffer.normal);
			m.setInt("NbMatId", sp4d.matIdManager.size())
		}

	}

	def package void makeView(Node guiNode) {
		// display1.move(0, 0, -1); // make it appear behind stats view
		for (var int i = 0; i < display.length; i++) {
				display.set(i, new Picture('''display«i»'''.toString))
		}
		sub = sp4d.onChange.subscribe([v|
			val w = v.vp.getCamera().getWidth()
			val h = v.vp.getCamera().getHeight()
			val scale = 1.0f / display.length

			for(var int i = 0; i < display.length; i++) {
				val displayEntry = display.get(i)
				displayEntry.setMaterial(getDebugMaterial(ViewKey.values().get(i)));
				displayEntry.setPosition(w * (i * scale), h * (1 - scale));
				displayEntry.setWidth(w * scale);
				displayEntry.setHeight(h * scale);
				if (!guiNode.hasChild(displayEntry)) {
					guiNode.attachChild(displayEntry);
				}
			}
			guiNode.updateGeometricState()
			guiNode.updateGeometricState()
		])
	}

	override void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app)
		makeView((app as SimpleApplication).getGuiNode())
	}

	override void cleanup() {
		super.cleanup()
		sub.unsubscribe()
		for (var int i = 0; i < display.length; i++) {
			if(display.get(i) !== null) {
				display.get(i).removeFromParent()
			}
		}
	}

	package Matrix4f m_ViewMatrixInverse = new Matrix4f()
	package Matrix4f m_ViewProjectionMatrixInverse = new Matrix4f()
	package Matrix4f m_ViewProjectionMatrix = new Matrix4f()
	package Vector3f m_FrustumCorner = new Vector3f()
	package Vector2f m_FrustumNearFar = new Vector2f()
	package Vector2f m_Resolution = new Vector2f()

	override void render(RenderManager rm) {
		if(sp4d === null || sp4d.vp === null) return;
		var Camera camera = sp4d.vp.getCamera()
		var int width = camera.getWidth()
		var int height = camera.getHeight()
		m_ViewMatrixInverse.set(camera.getViewMatrix()).invertLocal()
		m_ViewProjectionMatrix.set(camera.getViewProjectionMatrix())
		m_ViewProjectionMatrixInverse.set(camera.getViewProjectionMatrix()).invertLocal()
		var float farY = (camera.getFrustumTop() / camera.getFrustumNear()) * camera.getFrustumFar()
		var float farX = farY * (width as float / height as float)
		m_FrustumCorner.set(farX, farY, camera.getFrustumFar())
		m_FrustumNearFar.set(camera.getFrustumNear(), camera.getFrustumFar())
		var Vector4f m_ProjInfo = Helpers::projInfo(camera, width, height)
		var Vector3f m_ClipInfo = Helpers::clipInfo(camera)
		m_Resolution.set(width, height)
		for (Material m : mats) {
			m.setMatrix4("ViewMatrixInverse", m_ViewMatrixInverse)
			m.setMatrix4("ViewProjectionMatrixInverse", m_ViewProjectionMatrixInverse)
			m.setMatrix4("ViewProjectionMatrix", m_ViewProjectionMatrix)
			m.setVector3("ClipInfo", m_ClipInfo)
			m.setVector4("ProjInfo", m_ProjInfo)
			m.setVector2("Resolution", m_Resolution)
			m.setVector2("FrustumNearFar", m_FrustumNearFar)
			m.setVector3("FrustumCorner", m_FrustumCorner)
		}
		super.render(rm)
	}

}
