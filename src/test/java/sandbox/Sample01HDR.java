package sandbox;

import com.jme3.post.HDRRenderer;
import com.jme3.renderer.lwjgl.LwjglDisplayCustom;
import com.jme3.system.AppSettings;


public class Sample01HDR extends Sample01{

	public static void main(String[] args){
		AppSettings settings = new AppSettings(false);
		//settings.setStencilBits(8);
		settings.setCustomRenderer(LwjglDisplayCustom.class);
		Sample01HDR app = new Sample01HDR();
		app.setSettings(settings);
		app.start();
	}

	@Override
	public void simpleInitApp() {
		super.simpleInitApp();

		HDRRenderer hdrRender = new HDRRenderer(assetManager, renderer);
		hdrRender.setSamples(0);
		hdrRender.setMaxIterations(20);
		hdrRender.setExposure(0.87f);
		hdrRender.setThrottle(0.33f);
		viewPort.addProcessor(hdrRender);
	}
}

