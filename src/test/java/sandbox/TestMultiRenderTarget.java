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

import lombok.RequiredArgsConstructor;
import rx.Observable;
import rx.subjects.BehaviorSubject;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.input.ChaseCamera;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.post.SceneProcessor;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;

public class TestMultiRenderTarget extends SimpleApplication{

	public static void main(String[] args){
		TestMultiRenderTarget app = new TestMultiRenderTarget();
		app.start();
	}

	@Override
	public void simpleInitApp() {
		@SuppressWarnings("unused")
		Picture display = (true)? useDeferred() : null;
		Spatial target = makeScene(10, 10 , 10);
		makeLigths((display != null)? display : rootNode);
		setupCamera(target);
	}

	Spatial makeScene(int nbX, int nbY, int nbZ) {
		// create the geometry and attach it
		Geometry teaGeom = (Geometry) assetManager.loadModel("Models/Teapot/Teapot.obj");
		//teaGeom.setMaterial(new Material(assetManager, "sandbox/MatGBuf.j3md"));
		teaGeom.setMaterial(new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md"));
		teaGeom.scale(2);

		Node group = new Node("group");
		float step = 3f;
		int halfX = nbX / 2;
		int halfY = nbY / 2;
		int halfZ = nbZ / 2;
		for (int x = -halfX; x <= (nbX - halfX); x++) {
			for (int y = -halfY; y <= (nbY - halfY); y++) {
				for (int z = -halfZ; z <= (nbZ - halfZ); z++) {
					Spatial child = teaGeom.clone(false);
					child.setLocalTranslation(x * step, y * step, z * step);
					group.attachChild(child);
				}
			}
		}

		rootNode.attachChild(group);
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
		//chaseCam.setDragToRotate(false);
		chaseCam.setMinVerticalRotation((float)Math.PI / -2f + 0.001f);
		chaseCam.setInvertVerticalAxis(true);
	}

	Picture useDeferred() {
		SceneProcessor4GBuf sp4gbuf = new SceneProcessor4GBuf(assetManager);
		viewPort.addProcessor(sp4gbuf);
		return makeDisplayVP(sp4gbuf);
	}

	Picture makeDisplayVP(SceneProcessor4GBuf sp4gbuf) {
		Picture display1 = new Picture("Picture");
		//display1.move(0, 0, -1); // make it appear behind stats view
		Picture display2 = (Picture) display1.clone();
		Picture display3 = (Picture) display1.clone();
		Picture display4 = (Picture) display1.clone();
		Picture display  = (Picture) display1.clone();

		//final Material mat = new Material(assetManager, "sandbox/MatGBufDef.j3md");
		final Material mat = new Material(assetManager, "Common/MatDefs/Light/Deferred.j3md");
		display.setMaterial(mat);
		display.addControl(new AbstractControl() {
			@Override
			protected void controlUpdate(float tpf) {
				if (mat != null) {
					Matrix4f inverseViewProj = cam.getViewProjectionMatrix().invert();
					mat.setMatrix4("ViewProjectionMatrixInverse", inverseViewProj);
				}
			}
			@Override
			protected void controlRender(RenderManager rm, ViewPort vp) {
			}
		});

		sp4gbuf.onChange.subscribe((v) -> {
			mat.setTexture("DiffuseData",  v.diffuseData);
			mat.setTexture("SpecularData", v.specularData);
			mat.setTexture("NormalData",   v.normalData);
			mat.setTexture("DepthData",    v.depthData);

			float w = v.diffuseData.getImage().getWidth();
			float h = v.diffuseData.getImage().getHeight();

			display.setPosition(0, 0);
			display.setWidth(w);
			display.setHeight(h);

			display1.setTexture(assetManager, v.diffuseData, false);
			display2.setTexture(assetManager, v.normalData, false);
			display3.setTexture(assetManager, v.specularData, false);
			display4.setTexture(assetManager, v.depthData, false);

			display1.setPosition(0, 0);
			display2.setPosition(w/2, 0);
			display3.setPosition(0, h/2);
			display4.setPosition(w/2, h/2);

			display1.setWidth(w/2);
			display1.setHeight(h/2);

			display2.setWidth(w/2);
			display2.setHeight(h/2);

			display3.setWidth(w/2);
			display3.setHeight(h/2);

			display4.setWidth(w/2);
			display4.setHeight(h/2);

			guiNode.updateGeometricState();

		});
		guiViewPort.setClearFlags(true, true, true);

		guiNode.attachChild(display);
		//        guiNode.attachChild(display1);
		guiNode.attachChild(display2);
		//        guiNode.attachChild(display3);
		//        guiNode.attachChild(display4);
		guiNode.updateGeometricState();
		return display;
	}
}

@RequiredArgsConstructor
class SceneProcessor4GBuf implements SceneProcessor {
	private FrameBuffer fb;
	public Texture2D diffuseData, normalData, specularData, depthData;
	private BehaviorSubject<SceneProcessor4GBuf> onChange0 = BehaviorSubject.create();
	public Observable<SceneProcessor4GBuf> onChange = onChange0;
	public final AssetManager assetManager;
	private RenderManager rm;
	private String techOrig;

	public void initialize(RenderManager rm, ViewPort vp) {
		this.rm = rm;
		reshape(vp, vp.getCamera().getWidth(), vp.getCamera().getHeight());
	}

	public void reshape(ViewPort vp, int w, int h) {
		diffuseData  = new Texture2D(w, h, Format.RGBA8);
		normalData   = new Texture2D(w, h, Format.RGBA8);
		specularData = new Texture2D(w, h, Format.RGBA8);
		depthData    = new Texture2D(w, h, Format.Depth);

		fb = new FrameBuffer(w, h, 1);
		fb.setDepthTexture(depthData);
		fb.addColorTexture(diffuseData);
		fb.addColorTexture(normalData);
		fb.addColorTexture(specularData);
		fb.setMultiTarget(true);

		/*
		 * Marks pixels in front of the far light boundary
            Render back-faces of light volume
            Depth test GREATER-EQUAL
            Write to stencil on depth pass
            Skipped for very small distant lights
		 */

		/*
		 * Find amount of lit pixels inside the volume
             Start pixel query
             Render front faces of light volume
             Depth test LESS-EQUAL
             Don’t write anything – only EQUAL stencil test
		 */

		/*
		 * Enable conditional rendering
            Based on query results from previous stage
            GPU skips rendering for invisible lights
		 */

		/*
		 * Render front-faces of light volume
            Depth test - LESS-EQUAL
            Stencil test - EQUAL
            Runs only on marked pixels inside light
		 */

		vp.setOutputFrameBuffer(fb);
		//rm.getRenderer().setMainFrameBufferOverride(fb);
		onChange0.onNext(this);
	}

	public boolean isInitialized() {
		return diffuseData != null;
	}

	public void preFrame(float tpf) {
		techOrig = rm.getForcedTechnique();
		rm.setForcedTechnique("GBuf");
	}

	public void postQueue(RenderQueue rq) {
	}

	public void postFrame(FrameBuffer out) {
		rm.setForcedTechnique(techOrig);
	}

	public void cleanup() {
	}

}
