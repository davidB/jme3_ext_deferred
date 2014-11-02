package jme3_ext_deferred;

import java.util.LinkedList;
import java.util.List;

import com.jme3.material.Material;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.ui.Picture;

public class DebugTextureViewer {
	public enum ViewKey {
		normals
		,depths
		,matIds
		,mats
		,ao
		//,matTable
		;
	}

	final SceneProcessor4Deferred sp4d;
	private List<Material> mats = new LinkedList<>();


	public DebugTextureViewer(SceneProcessor4Deferred sp4d) {
		this.sp4d = sp4d;
		sp4d.onChange.subscribe(this::materialsUpdate);
	}

	public Material getDebugMaterial(ViewKey key) {
		Material m = new Material(sp4d.assetManager, "MatDefs/deferred/debug_gbuffer.j3md");
		m.getAdditionalRenderState().setDepthTest(false);
		m.getAdditionalRenderState().setDepthWrite(false);
		m.setBoolean("FullView", false);
		//m.selectTechnique("objgreen", rm);
		m.selectTechnique(key.name(), sp4d.rm);
		mats.add(m);
		materialsUpdate(sp4d);
		return m;
	}

	void materialsUpdate(SceneProcessor4Deferred sp) {
		Camera camera = sp.vp.getCamera();
		Vector3f m_FrustumCorner = new Vector3f();
		Matrix4f m_ViewProjectionMatrixInverse = new Matrix4f();
		float farY = (camera.getFrustumTop() / camera.getFrustumNear()) * camera.getFrustumFar();
		float farX = farY * ((float) camera.getWidth() / (float) camera.getHeight());
		m_FrustumCorner.set(farX, farY, sp.vp.getCamera().getFrustumFar());
		for(Material m : mats) {
			//Set<String> params = m.getMaterialDef().getMaterialParams().stream().map((mp) -> mp.getName()).collect(Collectors.toSet());
			m.setTexture("NormalBuffer", sp.pass4gbuffer.gbuffer.normal);
			m.setTexture("DepthBuffer", sp.pass4gbuffer.gbuffer.depth);
			m.setTexture("AOBuffer", sp.pass4ao.aobuffer.tex);
			m.setTexture("MatBuffer", sp.matIdManager.tableTex);
			m.setMatrix4("ViewProjectionMatrixInverse", m_ViewProjectionMatrixInverse);
			m.setVector3("FrustumCorner", m_FrustumCorner);
			m.setInt("NbMatId", sp4d.matIdManager.size());
		}
	}

	public void makeView(Node guiNode, ViewKey... keys) {
		Picture[] display = new Picture[keys.length];
		//display1.move(0, 0, -1); // make it appear behind stats view
		for(int i = 0; i < display.length; i++) {
			display[i] = new Picture("display" + i);
		}
		float scale = 1.0f / display.length;

		sp4d.onChange.subscribe((v) -> {
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

}
