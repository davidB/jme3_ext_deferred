package sandbox;

import jme3_ext_deferred.Helpers4Lights;
import jme3_ext_deferred.Helpers4Mesh;
import jme3_ext_deferred.MatIdManager;
import jme3_ext_deferred.MaterialConverter;
import lombok.RequiredArgsConstructor;
import rx_ext.Observable4AddRemove;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.material.Material;
import com.jme3.material.MaterialCustom;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;

@RequiredArgsConstructor
public class AppState4Sample01 extends AbstractAppState {
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

	final public MatIdManager matIdManager;
	final public Observable4AddRemove<Geometry> lights;
	AssetManager assetManager;

	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		assetManager = app.getAssetManager();
		Node anchor = new Node("Sample01");
		makeScene(anchor, 5, 7, 4);
		makeLigths(lights, anchor);
		((SimpleApplication) app).getRootNode().attachChild(anchor);
	}

	Spatial makeScene(Node anchor0, int nbX, int nbY, int nbZ) {
		Material matDef = new MaterialCustom(assetManager, "MatDefs/deferred/gbuffer.j3md");
		matDef.setInt("MatId", matIdManager.findMatId(ColorRGBA.Gray, ColorRGBA.White));

		Geometry[] geotmpl = new Geometry[]{
			(Geometry) assetManager.loadModel("Models/teapot.j3o")
			,new Geometry("box", new Box(0.5f, 0.5f, 0.5f))
			,new Geometry("sphere", new Sphere(16, 16, 0.5f))
		};
		Vector3f margin = new Vector3f(0.1f, 0.1f, 0.1f);
		Vector3f cellSizeMax = new Vector3f();
		for(Spatial child :geotmpl) {
			BoundingBox bb = (BoundingBox)child.getWorldBound();
			cellSizeMax.maxLocal(bb.getExtent(null).multLocal(2.0f));
		}
		cellSizeMax.addLocal(margin);
		Node group = new Node("group");
		for (int x = 0; x < nbX; x++) {
			for (int y = 0; y < nbY; y++) {
				for (int z = 0; z < nbZ; z++) {
					int i = z + y * nbZ + x * nbZ * nbY;
					Spatial child = geotmpl[i % geotmpl.length].clone();
					Vector3f pos = new Vector3f(cellSizeMax).multLocal(x,y,z);
					child.center();
					pos.addLocal(child.getLocalTranslation());
					child.setLocalTranslation(pos);
					Material mat = matDef.clone();
					mat.setInt("MatId", matIdManager.findMatId(colors[i % colors.length], ColorRGBA.White));
					mat.setColor("Color", colors[i % colors.length]);
					mat.setColor("Specular", ColorRGBA.White);
					child.setMaterial(mat);
					group.attachChild(child);
				}
			}
		}
		group.center();

		Spatial sponza = assetManager.loadModel("Models/crytek_sponza2.j3o");
		sponza.scale(10.0f);
		//sponza.setLocalTranslation(new Vector3f(-8.f, -0.25f, 0.f).multLocal(sponza.getWorldBound().getCenter()));
		sponza.setLocalTranslation(new Vector3f(0f, -8f + 1.5f, 0.f)); //-8 if the location of physical floor in Sample02 :-P
		group.attachChild(sponza);

		MaterialConverter mc = new MaterialConverter(assetManager, matIdManager);
//		mc.defaultMaterial = matDef;
		group.breadthFirstTraversal(mc);

		anchor0.attachChild(group);
		return group;
	}

	void makeLigths(Observable4AddRemove<Geometry> lights, Node anchor) {
		Geometry light0 = Helpers4Lights.newAmbiantLight("lambiant", new ColorRGBA(0.05f,0.05f,0.02f,1.0f), assetManager);
		anchor.attachChild(light0);
		lights.add.onNext(light0);


		Geometry light1 = Helpers4Lights.newDirectionnalLight("ldir", new Vector3f(-0.5f, -0.5f, -0.5f), new ColorRGBA(241f/255f*0.2f,215f/255f*0.2f,106f/255f*0.2f,1.0f), assetManager);
		anchor.attachChild(light1);
		lights.add.onNext(light1);

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
						Geometry pl = Helpers4Lights.asPointLight(new Geometry("pl"+i, mesh), colors[i % colors.length], assetManager, rangeY);
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
}

