package sandbox;

import jme3_ext_deferred.DebugTextureViewer;
import jme3_ext_deferred.MatIdManager;
import jme3_ext_deferred.SceneProcessor4Deferred;

import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
import com.jme3.input.ChaseCamera;
import com.jme3.input.JoyInput;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.TouchInput;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.lwjgl.LwjglDisplayCustom;
import com.jme3.renderer.lwjgl.LwjglRendererCustom;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext.Type;
import com.jme3.system.lwjgl.LwjglContext;
import com.jme3.system.lwjgl.LwjglDisplay;


public class Sample01 extends SimpleApplication{

	public static void main(String[] args){
		AppSettings settings = new AppSettings(false);
		//settings.setStencilBits(8);
		//settings.setCustomRenderer(LwjglDisplayCustom.class);
		Sample01 app = new Sample01();
		app.setSettings(settings);
		app.start();
	}

	private final MatIdManager matIdManager = new MatIdManager();
	private final ColorRGBA[] colors = new ColorRGBA[]{
			ColorRGBA.Red,
			ColorRGBA.Green,
			ColorRGBA.Blue,
			ColorRGBA.White,
			ColorRGBA.Cyan,
			ColorRGBA.DarkGray,
			ColorRGBA.Magenta,
			ColorRGBA.Orange,
			ColorRGBA.Pink,
			ColorRGBA.Yellow
	};

	@Override
	public void simpleInitApp() {
		SceneProcessor4Deferred sp4gbuf = useDeferred();
		//		viewPort.addProcessor(new SceneProcessor4Quad(assetManager));
		Spatial target = makeScene(rootNode, 2, 2, 2);
		makeLigths((sp4gbuf != null)? sp4gbuf.lightsRoot : rootNode);
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
		matDef.setInt("MatId", matIdManager.findMatId(ColorRGBA.Gray, ColorRGBA.White));

		Node pattern = new Node();
		pattern.attachChild((Geometry) assetManager.loadModel("Models/Teapot/Teapot.obj"));
		pattern.attachChild(new Geometry("box", new Box(0.5f, 0.5f, 0.5f)));
		pattern.attachChild(new Geometry("sphere", new Sphere(16, 16, 0.5f)));
		float deltaX = 0;
		int colorIdx = colors.length / 2;
		for(Spatial child :pattern.getChildren()) {
			Material mat = matDef.clone();
			mat.setInt("MatId", matIdManager.findMatId(colors[colorIdx++ % colors.length], ColorRGBA.White));
			child.setMaterial(mat);
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

			private Geometry[] pls = new Geometry[2];
			private Node anchor = null;

			@Override
			public void setSpatial(Spatial spatial) {
				super.setSpatial(spatial);
				if (anchor != null && anchor != spatial) {
					for (int i = 0; i < pls.length; i++){
						anchor.detachChild(pls[i]);
					}
					anchor = null;
				}
				if (spatial != null && anchor != spatial) {
					anchor = (Node)spatial;
					Material mat0 = assetManager.loadMaterial("Materials/deferred/lighting.j3m");
					for (int i = 0; i < pls.length; i++){
						Geometry pl = new Geometry("pl"+i, new Sphere(16, 16, 5f));
						Material mat = mat0.clone();
						pl.setMaterial(mat);
						mat.setColor("Color", colors[i % colors.length]);
						anchor.attachChild(pl);
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
					Geometry pl = pls[i];
					float angle = deltaItem * i + deltaTime;
					pl.setLocalTranslation(FastMath.cos(angle) * radius, 0, FastMath.sin(angle) * radius);
				}
			}

			@Override
			protected void controlRender(RenderManager rm, ViewPort vp) {
			}
		});
	}

	void setupCamera(Spatial target) {
		flyCam.setEnabled(false);
		ChaseCamera chaseCam = new ChaseCamera(cam, target, inputManager);
		chaseCam.setDefaultDistance(6.0f);
		chaseCam.setMaxDistance(100f);
		//chaseCam.setDragToRotate(false);
		chaseCam.setMinVerticalRotation((float)Math.PI / -2f + 0.001f);
		chaseCam.setInvertVerticalAxis(true);
		cam.setFrustumFar(1000.0f);
	}

	SceneProcessor4Deferred useDeferred() {
		SceneProcessor4Deferred sp4gbuf = new SceneProcessor4Deferred(assetManager, matIdManager);
		viewPort.addProcessor(sp4gbuf);
		DebugTextureViewer dbg = new DebugTextureViewer(sp4gbuf);
		dbg.makeView(guiNode, DebugTextureViewer.ViewKey.values());
		return sp4gbuf;
	}
}

