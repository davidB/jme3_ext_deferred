package sandbox

import com.jme3.app.SimpleApplication
import com.jme3.post.FilterPostProcessor
import com.jme3.system.AppSettings
import jme3_ext_deferred.AppState4ViewDeferredTexture
import jme3_ext_deferred.MatIdManager
import jme3_ext_deferred.SceneProcessor4Deferred
import com.jme3.app.BasicProfilerState
import jme3_ext_xbuf.Xbuf
import jme3_ext_remote_editor.AppState4RemoteCommand
import jme3_ext_xbuf.XbufLoader
import jme3_ext_xbuf.Loader4Materials
import org.slf4j.Logger
import com.jme3.material.Material
import jme3_ext_deferred.MaterialConverter

class MainRemote {
	def static void main(String[] args) throws Exception {
		// Logger.getLogger("").setLevel(Level.WARNING);
		val settings = new AppSettings(true)
		settings.setResolution(1280, 720)
		settings.setVSync(false)
		settings.setFullscreen(false)
		settings.setDepthBits(24)
		settings.gammaCorrection = true
		//settings.swapBuffers = false
		settings.setRenderer(AppSettings.LWJGL_OPENGL3)
		//settings.setRenderer(AppSettings.JOGL_OPENGL_FORWARD_COMPATIBLE)
		//settings.setAudioRenderer(AppSettings.JOAL)
		val app = new SimpleApplication() {
			override simpleInitApp() {
			}
		}
		app.setSettings(settings)
		app.setShowSettings(false)
		app.setDisplayFps(true)
		app.setDisplayStatView(true)
		app.start()
		val matIdManager = new MatIdManager()
		// Setup Camera
		app.enqueue([
			app.getFlyByCamera().setMoveSpeed(10)
			// app.getFlyByCamera().setEnabled(false);
//			app.getInputManager().setCursorVisible(false)
//			app.getCamera().setFrustumFar(1000.0f);
//			ChaseCamera chaseCam = new ChaseCamera(app.getCamera(), app.getRootNode(), app.getInputManager());
//			chaseCam.setDefaultDistance(6.0f);
//			chaseCam.setMaxDistance(100f);
//			//chaseCam.setDragToRotate(false);
//			chaseCam.setMinVerticalRotation((float)Math.PI / -2f + 0.001f);
//			chaseCam.setInvertVerticalAxis(true);
			return null
		]) // app.getFlyByCamera().setEnabled(false);
		app.enqueue([
		    val loader4Materials = new Loader4Materials(app.assetManager, null) {
		        val MaterialConverter mc = new MaterialConverter(app.assetManager, matIdManager)
		        override Material newMaterial(xbuf.Materials.Material m, Logger log) {
		           mc.toMaterialCustom(super.newMaterial(m, log))
                }
		    }
		    val xbuf = new Xbuf(app.getAssetManager(), null, loader4Materials, null)
		    XbufLoader.xbufFactory = [assetManager | xbuf]
            app.assetManager.registerLoader(typeof(XbufLoader), "xbuf")
            app.setPauseOnLostFocus(false); //<-- Required else remote application will not receive image (eg: blender freeze)  
            app.stateManager.attach(new AppState4RemoteCommand(4242, xbuf))
		])
		app.enqueue([
			val out = new SceneProcessor4Deferred(app.getAssetManager(), matIdManager)
			app.getViewPort().addProcessor(out)
			app.stateManager.attach(
				new AppState4ViewDeferredTexture(out, AppState4ViewDeferredTexture.ViewKey.values()))
			return out
		])
		app.enqueue([
			val fpp = new FilterPostProcessor(app.getAssetManager())
			// BloomFilter bf = new BloomFilter(BloomFilter.GlowMode.Scene);
			// fpp.addFilter(bf);
			//fpp.addFilter(new FXAAFilter())
			//fpp.addFilter(new FXAAFilter())
			// fpp.addFilter(new GammaCorrectionFilter())
			app.getViewPort().addProcessor(fpp)

			app.renderer.setMainFrameBufferSrgb(true)
			app.renderer.setLinearizeSrgbImages(true)
			return null
		])
		
		app.enqueue([
		    app.getStateManager().attach(new BasicProfilerState())
		    app.appProfiler
		])
	}

}
