package sandbox

import com.jme3.app.SimpleApplication
//import com.jme3.renderer.lwjgl.LwjglDisplayCustom;
import com.jme3.system.AppSettings

package class DumpInfo {
	def static void main(String[] args) {
		var AppSettings settings = new AppSettings(true)
		settings.setResolution(1280, 720)
		settings.setVSync(false)
		settings.setFullscreen(false)
		settings.setDepthBits(24) // settings.setStencilBits(8);
		settings.setRenderer(AppSettings.LWJGL_OPENGL3) // settings.setCustomRenderer(LwjglDisplayCustom.class);
		settings.put("GraphicsDebug", true) // settings.
		var SimpleApplication app = []
		app.setSettings(settings)
		app.setDisplayFps(true)
		app.setDisplayStatView(false)
		app.start()
	}

}
