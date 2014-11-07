package sandbox;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.input.ChaseCamera;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.OBJLoader;
import com.jme3.system.AppSettings;


public class SponzaScene extends SimpleApplication{

	public static void main(String[] args){
		AppSettings settings = new AppSettings(false);
		SponzaScene app = new SponzaScene();
		app.setSettings(settings);
		app.start();
	}

	@Override
	public void simpleInitApp() {
		Spatial target = makeScene(rootNode);
		makeLigths(rootNode);
		setupCamera(target);
	}

	Spatial makeScene(Node anchor0) {
		assetManager.registerLoader(OBJLoader.class, "obj");
		assetManager.registerLocator(System.getProperty("user.home"), FileLocator.class);
		//Spatial sponza = assetManager.loadModel("Téléchargements/t/crytek/sponza.obj");
		Spatial sponza = assetManager.loadModel("Models/Sponza/Sponza.j3o");
		anchor0.attachChild(sponza);
		return anchor0;
	}

	void makeLigths(Node anchor) {
		DirectionalLight dl = new DirectionalLight();
		dl.setColor(ColorRGBA.White);
		dl.setDirection(Vector3f.UNIT_XYZ.negate());

		anchor.addLight(dl);
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
}

