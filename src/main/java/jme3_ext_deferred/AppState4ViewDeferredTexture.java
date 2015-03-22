package jme3_ext_deferred;

import java.util.LinkedList;
import java.util.List;

import rx.Subscription;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.material.Material;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Node;
import com.jme3.ui.Picture;

public class AppState4ViewDeferredTexture extends AbstractAppState {
	public enum ViewKey {
		normals
		,depths
		,albedos
		,speculars
		,matIds
		,mats
		,ao
		,positions
		,lights
		,texture
		//,stencil // doesn't work may require investigation / extension like https://www.opengl.org/registry/specs/ARB/stencil_texturing.txt
		;
	}

	final SceneProcessor4Deferred sp4d;
	final ViewKey[] keys;
	final Picture[] display;
	private Subscription sub;
	private List<Material> mats = new LinkedList<>();

	public AppState4ViewDeferredTexture(SceneProcessor4Deferred sp4d, ViewKey... keys) {
		this.sp4d = sp4d;
		this.keys = keys;
		this.display = new Picture[keys.length];
		sp4d.onChange.subscribe(this::materialsUpdate);
	}

	Material getDebugMaterial(ViewKey key) {
		Material m = new Material(sp4d.assetManager, "MatDefs/debug/debug_gbuffer.j3md");
		m.getAdditionalRenderState().setDepthTest(false);
		m.getAdditionalRenderState().setDepthWrite(false);
		m.setBoolean("FullView", false);
		//m.selectTechnique("objgreen", sp4d.rm);
		//m.selectTechnique("fullgreen", sp4d.rm);
		m.selectTechnique(key.name(), sp4d.rm);
		mats.add(m);
		materialsUpdate(sp4d);
		return m;
	}

	void materialsUpdate(SceneProcessor4Deferred sp) {
		for(Material m : mats) {
			//Set<String> params = m.getMaterialDef().getMaterialParams().stream().map((mp) -> mp.getName()).collect(Collectors.toSet());
			m.setTexture("NormalBuffer", sp.pass4gbuffer.gbuffer.normal);
			m.setTexture("DepthBuffer", sp.pass4gbuffer.gbuffer.depth);
			m.setTexture("AOBuffer", sp.pass4ao.aobuffer.tex);
			m.setTexture("MatBuffer", sp.matIdManager.tableTex);
			m.setTexture("AlbedoBuffer", sp.pass4gbuffer.gbuffer.albedo);
			m.setTexture("SpecularBuffer", sp.pass4gbuffer.gbuffer.specular);
			m.setTexture("LBuffer", sp.pass4lbuffer.lbuffer.tex);
			m.setTexture("Texture", sp.pass4lbuffer.ligthPeerTmpl4SpotAndShadow.shadowMapGen.shadowMap0);
			//m.setTexture("Texture", sp.pass4gbuffer.gbuffer.normal);
			m.setInt("NbMatId", sp4d.matIdManager.size());
		}
	}

	void makeView(Node guiNode) {
		//display1.move(0, 0, -1); // make it appear behind stats view
		for(int i = 0; i < display.length; i++) {
			display[i] = new Picture("display" + i);
		}
		float scale = 1.0f / display.length;

		sub = sp4d.onChange.subscribe((v) -> {
			float w = v.vp.getCamera().getWidth();
			float h = v.vp.getCamera().getHeight();

			for(int i = 0; i < display.length; i++) {
				display[i].setMaterial(getDebugMaterial(ViewKey.values()[i]));
				display[i].setPosition(w * (i * scale), h * (1 - scale));
				display[i].setWidth(w * scale);
				display[i].setHeight(h * scale);
				if (!guiNode.hasChild(display[i])) {
					guiNode.attachChild(display[i]);
				}
			}
			guiNode.updateGeometricState();
			guiNode.updateGeometricState();
		});
	}

	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);
		makeView(((SimpleApplication)app).getGuiNode());
	}

	@Override
	public void cleanup() {
		super.cleanup();
		sub.unsubscribe();
		for(int i = 0; i < display.length; i++) {
			if (display[i] != null) display[i].removeFromParent();
		}
	}

	Matrix4f m_ViewMatrixInverse = new Matrix4f();
	Matrix4f m_ViewProjectionMatrixInverse = new Matrix4f();
	Matrix4f m_ViewProjectionMatrix = new Matrix4f();
	Vector3f m_FrustumCorner = new Vector3f();
	Vector2f m_FrustumNearFar = new Vector2f();
	Vector2f m_Resolution = new Vector2f();

	@Override
	public void render(RenderManager rm) {
		if (sp4d == null || sp4d.vp == null) return;
		Camera camera = sp4d.vp.getCamera();
		int width = camera.getWidth();
		int height = camera.getHeight();
		m_ViewMatrixInverse.set(camera.getViewMatrix()).invertLocal();
		m_ViewProjectionMatrix.set(camera.getViewProjectionMatrix());
		m_ViewProjectionMatrixInverse.set(camera.getViewProjectionMatrix()).invertLocal();
		float farY = (camera.getFrustumTop() / camera.getFrustumNear()) * camera.getFrustumFar();
		float farX = farY * ((float) width / (float) height);
		m_FrustumCorner.set(farX, farY, camera.getFrustumFar());
		m_FrustumNearFar.set(camera.getFrustumNear(), camera.getFrustumFar());
		Vector4f m_ProjInfo = Helpers.projInfo(camera, width, height);
		Vector3f m_ClipInfo = Helpers.clipInfo(camera);
		m_Resolution.set(width, height);
		for(Material m : mats) {
			m.setMatrix4("ViewMatrixInverse", m_ViewMatrixInverse);
			m.setMatrix4("ViewProjectionMatrixInverse", m_ViewProjectionMatrixInverse);
			m.setMatrix4("ViewProjectionMatrix", m_ViewProjectionMatrix);
			m.setVector3("ClipInfo", m_ClipInfo);
			m.setVector4("ProjInfo", m_ProjInfo);
			m.setVector2("Resolution", m_Resolution);
			m.setVector2("FrustumNearFar", m_FrustumNearFar);
			m.setVector3("FrustumCorner", m_FrustumCorner);
		}
		super.render(rm);
	}
}
