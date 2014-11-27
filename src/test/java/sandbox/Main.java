package sandbox;

import java.util.logging.Level;
import java.util.logging.Logger;

import jme3_ext_deferred.AppState4ViewDeferredTexture;
import jme3_ext_deferred.MatIdManager;
import jme3_ext_deferred.SceneProcessor4Deferred;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.input.ChaseCamera;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.renderer.lwjgl.LwjglDisplayCustom;
import com.jme3.system.AppSettings;

public class Main {
	public static void main(String[] args) throws Exception {
		Logger.getLogger("").setLevel(Level.WARNING);

		AppSettings settings = new AppSettings(true);
		settings.setResolution(1280, 720);
		settings.setVSync(false);
		settings.setFullscreen(false);
		settings.setDepthBits(24);
		settings.setCustomRenderer(LwjglDisplayCustom.class);

		SimpleApplication app = new SimpleApplication(){
			@Override
			public void simpleInitApp() {
			}

		};
		app.setSettings(settings);
		app.setShowSettings(false);
		app.start();

		final MatIdManager matIdManager = new MatIdManager();
		//Setup Camera
		app.enqueue(() -> {
			app.getFlyByCamera().setEnabled(false);
			app.getCamera().setFrustumFar(1000.0f);
			ChaseCamera chaseCam = new ChaseCamera(app.getCamera(), app.getRootNode(), app.getInputManager());
			chaseCam.setDefaultDistance(6.0f);
			chaseCam.setMaxDistance(100f);
			//chaseCam.setDragToRotate(false);
			chaseCam.setMinVerticalRotation((float)Math.PI / -2f + 0.001f);
			chaseCam.setInvertVerticalAxis(true);
			return null;
		});

		// blocking until ready
		SceneProcessor4Deferred sp4gbuf = app.enqueue(() -> {
			SceneProcessor4Deferred out = new SceneProcessor4Deferred(app.getAssetManager(), matIdManager);
			app.getViewPort().addProcessor(out);
			app.getStateManager().attach(new AppState4ViewDeferredTexture(out, AppState4ViewDeferredTexture.ViewKey.values()));
			return out;
		}).get();

		app.enqueue(() -> {
			app.getStateManager().attach(new AppState4Sample01(matIdManager, sp4gbuf.lights.ar));
			return null;
		});
		app.enqueue(() -> {
			app.getStateManager().attach(new AppState4Sample02_BrokenCube(matIdManager, sp4gbuf.lights.ar));
			return null;
		});
//		app.enqueue(() -> {
//			app.getStateManager().attach(new AppState4RegularHDR());
//			return null;
//		});

		app.enqueue(() -> {
			FilterPostProcessor fpp = new FilterPostProcessor(app.getAssetManager());
			BloomFilter bf = new BloomFilter(BloomFilter.GlowMode.Scene);
			//fpp.addFilter(bf);
			//fpp.addFilter(new FXAAFilter());
			//fpp.addFilter(new FXAAFilter());
			app.getViewPort().addProcessor(fpp);
			return null;
		});


	}
}