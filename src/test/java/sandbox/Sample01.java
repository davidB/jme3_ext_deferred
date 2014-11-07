package sandbox;

import jme3_ext_deferred.DebugTextureViewer;
import jme3_ext_deferred.Helpers4Lights;
import jme3_ext_deferred.Helpers4Mesh;
import jme3_ext_deferred.MatIdManager;
import jme3_ext_deferred.MaterialConverter;
import jme3_ext_deferred.SceneProcessor4Deferred;
import rx_ext.Observable4AddRemove;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.bounding.BoundingBox;
import com.jme3.input.ChaseCamera;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.lwjgl.LwjglDisplayCustom;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.plugins.OBJLoader;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;


public class Sample01 extends SimpleApplication{

	public static void main(String[] args){
		AppSettings settings = new AppSettings(false);
		//settings.setStencilBits(8);
		settings.setCustomRenderer(LwjglDisplayCustom.class);
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
		makeLigths(sp4gbuf.lights.ar, rootNode);
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
			mat.setColor("Albedo", colors[colorIdx++ % colors.length]);
			mat.setColor("Specular", ColorRGBA.White);
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

		MaterialConverter mc = new MaterialConverter(assetManager, matIdManager);
//		mc.defaultMaterial = matDef;
		assetManager.registerLoader(OBJLoader.class, "obj");
		assetManager.registerLocator(System.getProperty("user.home"), FileLocator.class);
		Spatial sponza = assetManager.loadModel("Téléchargements/t/crytek/sponza.obj");
		sponza.scale(0.1f);
//		Spatial sponza = assetManager.loadModel("Models/Sponza/Sponza.j3o");
		sponza.setLocalTranslation(new Vector3f(-8.f, -0.25f, 0.f).multLocal(sponza.getWorldBound().getCenter()));
		sponza.breadthFirstTraversal(mc);
		group.attachChild(sponza);
		anchor0.attachChild(group);
		return group;
	}

	void makeLigths(Observable4AddRemove<Geometry> lights, Node anchor) {
		//		DirectionalLight dl = new DirectionalLight();
		//		dl.setColor(ColorRGBA.White);
		//		dl.setDirection(Vector3f.UNIT_XYZ.negate());
		//
		//		anchor.addLight(dl);

		//Directionnal Light
		Geometry light0 = Helpers4Lights.newDirectionnalLight("ldir", new Vector3f(-0.5f, -0.5f, -0.5f), new ColorRGBA(0.2f,0.2f,0.2f,1.0f), assetManager);
		//Geometry light0 = Helpers4Lights.newAmbiantLight("lambiant", new ColorRGBA(0.2f,0.2f,0.2f,1.0f), assetManager);
		anchor.attachChild(light0);
		lights.add.onNext(light0);

		anchor.addControl(new AbstractControl() {

			private Geometry[] pls = new Geometry[8];
			private Node anchor = null;
			float radius = 10f;
			float rangeY = -50;

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
					//Mesh mesh = new Cylinder(16, 16, radius, 50f);
					//Mesh mesh = new Sphere(16, 16, radius);
					//Quaternion rot = new Quaternion(new float[]{(float)(0.5f * Math.PI), 0f, 0f}); // to have vertical cylinder
					Mesh mesh = Helpers4Mesh.newCone(4, rangeY, radius);
					Quaternion rot = new Quaternion();

					for (int i = 0; i < pls.length; i++){
						//Geometry pl = new Geometry("pl"+i, new Sphere(16, 16, radius));
						Geometry pl = Helpers4Lights.asPointLight(new Geometry("pl"+i, mesh), colors[i % colors.length], assetManager);
						pl.setLocalRotation(rot);
						anchor.attachChild(pl);
						lights.add.onNext(pl);
						pls[i] = pl;
					}
				}
			}

			@Override
			protected void controlUpdate(float tpf) {
				// helix x spiral ?
//				float deltaItem = (float)(2f * Math.PI / pls.length);
//				float deltaTime = 0;//(float)Math.PI * (timer.getTimeInSeconds() % 6) / 3; // 3s for full loop
//				for (int i = 0; i < pls.length; i++){
//					Geometry pl = pls[i];
//					float angle = deltaItem * i + deltaTime;
//					float d = radius*1.5f;
//					pl.setLocalTranslation(FastMath.cos(angle) * d, -rangeY, FastMath.sin(angle) * d);
//				}
//
				// grid ?
				int nbSize = (int) Math.ceil(Math.sqrt((double)pls.length));
				for (int x = 0; x < nbSize; x++){
					for (int z = 0; z < nbSize; z++){
						int i = x + z * nbSize;
						if (i < pls.length) {
							Geometry pl = pls[i];
							pl.setLocalTranslation((x - nbSize/2) * 2f * radius, -rangeY * 0.5f, (z - nbSize/2) * 2f * radius);
						}
					}
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

