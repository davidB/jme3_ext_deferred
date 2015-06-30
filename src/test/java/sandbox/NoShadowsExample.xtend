package sandbox

import com.jme3.app.SimpleApplication
import com.jme3.light.PointLight
import com.jme3.material.Material
import com.jme3.math.ColorRGBA
import com.jme3.math.Vector3f
import com.jme3.post.FilterPostProcessor
import com.jme3.renderer.queue.RenderQueue
import com.jme3.scene.Geometry
import com.jme3.scene.shape.Box
import com.jme3.scene.shape.Sphere
import com.jme3.shadow.EdgeFilteringMode
import com.jme3.shadow.PointLightShadowFilter
import com.jme3.shadow.PointLightShadowRenderer

class NoShadowsExample extends SimpleApplication {
	def static void main(String[] args) {
		new NoShadowsExample().start()
	}

	override void simpleInitApp() {
		// Setting up cam and its controls
		flyCam.setEnabled(false)
		flyCam.setMoveSpeed(30)
		cam.setFrame(new Vector3f(0f, 1f, 6f), new Vector3f(-1f, 0f, 0f), new Vector3f(0f, 1f, 0f),	new Vector3f(0f, 0f, -1f))
		// LightGray material for floor and sphere
		val material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md")
		material.setColor("Ambient", ColorRGBA.LightGray)
		// Floor, 20x20 at 0,0,0
		val floor = new Geometry("Floor", new Box(10, 0.001f, 10))
		floor.setMaterial(material)
		floor.setShadowMode(RenderQueue.ShadowMode.CastAndReceive)
		rootNode.attachChild(floor)
		// Sphere
		val sphere = new Geometry("Sphere", new Sphere(50, 50, 0.5f))
		sphere.move(0f, 1f, 0f)
		sphere.setMaterial(material)
		sphere.setShadowMode(RenderQueue.ShadowMode.Cast)
		rootNode.attachChild(sphere)
		// White PointLight at (0,5,0)
		val light = new PointLight()
		light.setColor(ColorRGBA.White)
		light.setPosition(new Vector3f(0f, 5f, 0f))
		light.setRadius(100)
		rootNode.addLight(light)
		// PointLightShadowRenderer
		val plsr = new PointLightShadowRenderer(assetManager, 1024)
		plsr.setLight(light)
		plsr.displayDebug()
		plsr.displayFrustum()
		plsr.setEdgeFilteringMode(EdgeFilteringMode.PCF4)
		viewPort.addProcessor(plsr)
		// PointLightShadowFilter (commented out, but doesn't work either)
		{
			/*
			 *             PointLightShadowFilter plsf = new PointLightShadowFilter(assetManager, 1024);
			 *             plsf.setLight(light);
			 *             plsf.setEnabled(true);
			 *             FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
			 *             fpp.addFilter(plsf);
			 *             viewPort.addProcessor(fpp);
			 */
		}

	}

}