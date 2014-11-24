package sandbox;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.HashSet;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.MaterialKey;
import com.jme3.asset.TextureKey;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.input.ChaseCamera;
import com.jme3.light.DirectionalLight;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.OBJLoader;
import com.jme3.shader.VarType;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture2D;


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
		Spatial sponza;
//		assetManager.registerLoader(OBJLoader.class, "obj");
//		assetManager.registerLocator(System.getProperty("user.home"), FileLocator.class);
//		sponza = assetManager.loadModel("Téléchargements/t/crytek/sponza.obj");
//		//sponza = assetManager.loadModel("Models/Sponza/Sponza.j3o");
//		File froot = new File("build/generated-sources/assets/");
//		Collection<File> files = save( sponza, "sponza", true, froot);

		assetManager.unregisterLoader(OBJLoader.class);
		assetManager.unregisterLocator(System.getProperty("user.home"), FileLocator.class);
		sponza = load("sponza");
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
		chaseCam.setMaxDistance(1000f);
		//chaseCam.setDragToRotate(false);
		chaseCam.setMinVerticalRotation((float)Math.PI / -2f + 0.001f);
		chaseCam.setInvertVerticalAxis(true);
		cam.setFrustumFar(1000.0f);
	}

	Collection<File> save(Spatial root, String name, boolean prefixTexture, File froot) {
		HashSet<File> b = new HashSet<File>();
		froot = froot.getAbsoluteFile();
		try {
			BinaryExporter exporter = new BinaryExporter();
			MaterialCollector mc = new MaterialCollector();
			mc.collect(root, true);
			for(Texture2D t : mc.texture2Ds){
				TextureKey ksrc = (TextureKey)t.getKey();
				File f = new File(froot, ksrc.getName());
				if (!f.exists()) {
					String kdestName = ksrc.getName().replace(ksrc.getFolder(), "Textures/" + (prefixTexture?name + "/" : ""));
					TextureKey kdest = new TextureKey(kdestName);
					kdest.setAnisotropy(ksrc.getAnisotropy());
					kdest.setAsCube(ksrc.isAsCube());
					kdest.setAsTexture3D(ksrc.isAsTexture3D());
					kdest.setFlipY(ksrc.isFlipY());
					kdest.setGenerateMips(ksrc.isGenerateMips());
					kdest.setTextureTypeHint(ksrc.getTextureTypeHint());
					t.setKey(kdest);
					f = new File(froot, kdest.getName());
					f.getParentFile().mkdirs();
					Files.copy(assetManager.locateAsset(ksrc).openStream(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
				}
				b.add(f);
			}
//			int count = 0;
			for(Material t : mc.materials) {
				MaterialKey k = (MaterialKey)t.getKey();
				if (k == null) {
//					//TODO create name from checksum
//					count++;
//					k = new AssetKey<Material>("Materials/" + count + ".j3m");
//					t.setKey(k);
//					File f = new File(froot, k.getName());
//					if (!f.exists()) {
//						f.getParentFile().mkdirs();
//						exporter.save(t, f);
//					}
//					b.add(f);
				} else {
					File f = new File(froot, k.getName());
					if (!f.exists()) {
						f.getParentFile().mkdirs();
						Files.copy(assetManager.locateAsset(k).openStream(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
					}
					b.add(f);
				}
			}
			File f0 = new File(froot, "Models/" + name +".j3o");
			exporter.save(root, f0);
			b.add(f0);
			return b;
		} catch(RuntimeException exc) {
			throw exc;
		} catch(Exception exc) {
			throw new RuntimeException("wrap:" + exc, exc);
		}
	}

	Spatial load(String name) {
		try {
			BinaryImporter importer = new BinaryImporter();
			importer.setAssetManager(assetManager);
			//importer.setAssetManager(new DesktopAssetManager());
			return (Spatial) importer.load(new File("build/generated-sources/assets/Models/" + name +".j3o"));
		} catch(RuntimeException exc) {
			throw exc;
		} catch(Exception exc) {
			throw new RuntimeException("wrap:" + exc, exc);
		}
	}

	static class MaterialCollector implements SceneGraphVisitor {
		final HashSet<Material> materials = new HashSet<Material>();
		final HashSet<Texture2D> texture2Ds = new HashSet<Texture2D>();

		public void collect(Spatial v, boolean reset) {
			if (reset) {
				materials.clear();
				texture2Ds.clear();
			}
			v.breadthFirstTraversal(this);
		}

		@Override
		public void visit(Spatial v) {
			if (v instanceof Geometry) {
				Geometry g = (Geometry)v;
				Material m = g.getMaterial();
				for (MatParam mp : m.getParams()) {
					if (mp.getVarType() == VarType.Texture2D) {
						texture2Ds.add(((Texture2D)mp.getValue()));
					}
				}
				materials.add(m);
			}
		}
	}
}

