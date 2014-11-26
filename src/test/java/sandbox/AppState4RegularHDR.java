package sandbox;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.post.HDRRenderer;


public class AppState4RegularHDR extends AbstractAppState{

	@Override
	public void initialize(AppStateManager stateManager, Application app) {

		HDRRenderer hdrRender = new HDRRenderer(app.getAssetManager(), app.getRenderer());
		hdrRender.setSamples(0);
		hdrRender.setMaxIterations(20);
		hdrRender.setExposure(0.87f);
		hdrRender.setThrottle(0.33f);
		app.getViewPort().addProcessor(hdrRender);
	}
}

