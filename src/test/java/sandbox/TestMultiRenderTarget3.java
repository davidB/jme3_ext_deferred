/*
 * Copyright (c) 2009-2012 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package sandbox;

import jme3_ext_deferred.DebugMaterialKey;
import jme3_ext_deferred.SceneProcessor4Deferred;

import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
import com.jme3.input.ChaseCamera;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.ui.Picture;


public class TestMultiRenderTarget3 extends SimpleApplication{

	public static void main(String[] args){
		TestMultiRenderTarget3 app = new TestMultiRenderTarget3();
		app.start();
	}

	@Override
	public void simpleInitApp() {
		@SuppressWarnings("unused")
		Picture display = (true)? useDeferred() : null;
		//		viewPort.addProcessor(new SceneProcessor4Quad(assetManager));
		Spatial target = makeScene(rootNode, 2, 2, 2);
		makeLigths((display != null)? display : rootNode);
		setupCamera(target);

		//		Geometry finalQuad = new Geometry("finalQuad", new Quad(1, 1));
		//		finalQuad.setLocalTranslation(0, 0, 0);
		//		//finalQuad.setIgnoreTransform(true);
		//		final Material mat = new Material(assetManager, "sandbox/MatGBufDef.j3md");
		//		finalQuad.setMaterial(mat);
		//		rootNode.attachChild(finalQuad);
		//stateManager.attach(new vdrones.AppStatePostProcessing());
	}

	Spatial makeScene(Node anchor0, int nbX, int nbY, int nbZ) {
		Material matDef = new Material(assetManager, "MatDefs/deferred/gbuffer.j3md");

		Node pattern = new Node();
		pattern.attachChild((Geometry) assetManager.loadModel("Models/Teapot/Teapot.obj"));
		pattern.attachChild(new Geometry("box", new Box(0.5f, 0.5f, 0.5f)));
		pattern.attachChild(new Geometry("sphere", new Sphere(16, 16, 0.5f)));
		float deltaX = 0;
		for(Spatial child :pattern.getChildren()) {
			child.setMaterial(matDef);
			BoundingBox bb = (BoundingBox)child.getWorldBound();
			child.setLocalTranslation(deltaX, 0, 0);
			deltaX += 2 * bb.getXExtent();
		}

		Node group = new Node("group");
		BoundingBox bb = (BoundingBox)pattern.getWorldBound();
		Vector3f size = new Vector3f(bb.getXExtent() + 0.2f, bb.getYExtent() + 0.2f, bb.getZExtent() + 0.2f);
		size.multLocal(2.0f);
		int halfX = nbX / 2;
		int halfY = nbY / 2;
		int halfZ = nbZ / 2;
		for (int x = -halfX; x <= (nbX - halfX); x++) {
			for (int y = -halfY; y <= (nbY - halfY); y++) {
				for (int z = -halfZ; z <= (nbZ - halfZ); z++) {
					Spatial child = pattern.clone(false);
					Vector3f pos = new Vector3f(x, y, z);
					child.setLocalTranslation(pos.mult(size));
					group.attachChild(child);
				}
			}
		}
		Spatial sponza = assetManager.loadModel("Models/Sponza/Sponza.j3o");
		sponza.setLocalTranslation(new Vector3f(-8.f, -0.25f, 0.f).multLocal(sponza.getWorldBound().getCenter()));
		sponza.setMaterial(matDef);
		group.attachChild(sponza);
		anchor0.attachChild(group);
		return group;
	}

	void makeLigths(Spatial anchor) {
		//		DirectionalLight dl = new DirectionalLight();
		//		dl.setColor(ColorRGBA.White);
		//		dl.setDirection(Vector3f.UNIT_XYZ.negate());
		//
		//		anchor.addLight(dl);

		anchor.addControl(new AbstractControl() {
			ColorRGBA[] colors = new ColorRGBA[]{
					ColorRGBA.White,
					ColorRGBA.Blue,
					ColorRGBA.Cyan,
					ColorRGBA.DarkGray,
					ColorRGBA.Green,
					ColorRGBA.Magenta,
					ColorRGBA.Orange,
					ColorRGBA.Pink,
					ColorRGBA.Red,
					ColorRGBA.Yellow
			};
			private PointLight[] pls = new PointLight[3];
			private Spatial anchor = null;

			@Override
			public void setSpatial(Spatial spatial) {
				super.setSpatial(spatial);
				if (anchor != null && anchor != spatial) {
					for (int i = 0; i < pls.length; i++){
						anchor.removeLight(pls[i]);
					}
					anchor = null;
				}
				if (spatial != null && anchor != spatial) {
					anchor = spatial;
					for (int i = 0; i < pls.length; i++){
						PointLight pl = new PointLight();
						pl.setColor(colors[i % colors.length]);
						pl.setRadius(5);
						anchor.addLight(pl);
						pls[i] = pl;
					}
				}
			}

			@Override
			protected void controlUpdate(float tpf) {
				float deltaItem = (float)(2f * Math.PI / pls.length);
				float deltaTime = (float)Math.PI * (timer.getTimeInSeconds() % 6) / 3; // 3s for full loop
				float radius = 3f;
				for (int i = 0; i < pls.length; i++){
					PointLight pl = pls[i];
					float angle = deltaItem * i + deltaTime;
					pl.setPosition( new Vector3f(FastMath.cos(angle) * radius, 0, FastMath.sin(angle) * radius));
				}
			}

			@Override
			protected void controlRender(RenderManager rm, ViewPort vp) {
			}
		});
	}

	public void setupCamera(Spatial target) {
		flyCam.setEnabled(false);
		ChaseCamera chaseCam = new ChaseCamera(cam, target, inputManager);
		chaseCam.setDefaultDistance(6.0f);
		chaseCam.setMaxDistance(100f);
		//chaseCam.setDragToRotate(false);
		chaseCam.setMinVerticalRotation((float)Math.PI / -2f + 0.001f);
		chaseCam.setInvertVerticalAxis(true);
		cam.setFrustumFar(1000.0f);
	}

	Picture useDeferred() {
		SceneProcessor4Deferred sp4gbuf = new SceneProcessor4Deferred(assetManager);
		viewPort.addProcessor(sp4gbuf);
		return makeDisplayVP(sp4gbuf);
	}

	Picture makeDisplayVP(SceneProcessor4Deferred sp4gbuf) {
		Picture[] display = new Picture[DebugMaterialKey.values().length];
		//display1.move(0, 0, -1); // make it appear behind stats view
		for(int i = 0; i < display.length; i++) {
			display[i] = new Picture("display" + i);
		}
		float scale = 0.25f;

		sp4gbuf.onChange.subscribe((v) -> {
			float w = v.vp.getCamera().getWidth();
			float h = v.vp.getCamera().getHeight();

			for(int i = 0; i < display.length; i++) {
				display[i].setMaterial(v.getDebugMaterial(DebugMaterialKey.values()[i]));
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
		return null;
	}
}

//@RequiredArgsConstructor
//class SceneProcessor4Quad implements SceneProcessor {
//	public final AssetManager assetManager;
//	private RenderManager rm;
//	private ViewPort vp;
//	private Geometry finalQuad;
//
//	public void initialize(RenderManager rm, ViewPort vp) {
//		this.rm = rm;
//		this.vp = vp;
//		reshape(vp, vp.getCamera().getWidth(), vp.getCamera().getHeight());
//
//		finalQuad = new Geometry("finalQuad", new Quad(1, 1));
//		finalQuad.setLocalTranslation(0, 0, 0);
//		//finalQuad.setIgnoreTransform(true);
//		final Material mat = new Material(assetManager, "sandbox/MatGBufDef.j3md");
//		finalQuad.setMaterial(mat);
//		finalQuad.setCullHint(Spatial.CullHint.Never);
//		mat.getAdditionalRenderState().setDepthTest(false);
//		mat.getAdditionalRenderState().setDepthWrite(false);
//		mat.selectTechnique("fullgreen", rm);
//		finalQuad.addControl(new AbstractControl() {
//			@Override
//			protected void controlUpdate(float tpf) {
//				if (mat != null) {
//					Matrix4f inverseViewProj = vp.getCamera().getViewProjectionMatrix().invert();
//					mat.setMatrix4("ViewProjectionMatrixInverse", inverseViewProj);
//				}
//			}
//			@Override
//			protected void controlRender(RenderManager rm, ViewPort vp) {
//			}
//		});
//
//		finalQuad.setQueueBucket(Bucket.Opaque);
//	}
//
//	public void reshape(ViewPort vp, int w, int h) {
//	}
//
//	public boolean isInitialized() {
//		return finalQuad != null;
//	}
//
//	public void preFrame(float tpf) {
//		finalQuad.updateLogicalState(tpf);
//		finalQuad.updateGeometricState();
//	}
//
//	public void postQueue(RenderQueue rq) {
//		rm.renderGeometry(finalQuad);
//	}
//
//	public void postFrame(FrameBuffer out) {
//
//	}
//
//	public void cleanup() {
//	}
//
//}
