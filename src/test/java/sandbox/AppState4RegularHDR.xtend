package sandbox

import com.jme3.app.Application
import com.jme3.app.state.AbstractAppState
import com.jme3.app.state.AppStateManager
import com.jme3.post.HDRRenderer

class AppState4RegularHDR extends AbstractAppState {
	override void initialize(AppStateManager stateManager, Application app) {
		var HDRRenderer hdrRender = new HDRRenderer(app.getAssetManager(), app.getRenderer())
		hdrRender.setSamples(0)
		hdrRender.setMaxIterations(20)
		hdrRender.setExposure(0.87f)
		hdrRender.setThrottle(0.33f)
		app.getViewPort().addProcessor(hdrRender)
	}

}
