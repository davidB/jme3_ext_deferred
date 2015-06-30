package sandbox

import jme3_ext_deferred.AppState4ViewDeferredTexture
import jme3_ext_deferred.MatIdManager
import jme3_ext_deferred.MaterialConverter
import jme3_ext_deferred.SceneProcessor4Deferred
import com.jme3.app.SimpleApplication
import com.jme3.light.AmbientLight
import com.jme3.light.DirectionalLight
import com.jme3.light.PointLight
import com.jme3.light.SpotLight
import com.jme3.material.Material
import com.jme3.math.ColorRGBA
import com.jme3.math.FastMath
import com.jme3.math.Vector3f
import com.jme3.post.SceneProcessor
import com.jme3.renderer.queue.RenderQueue
import com.jme3.scene.Geometry
import com.jme3.scene.SimpleBatchNode
import com.jme3.scene.shape.Box
import com.jme3.system.AppSettings

//import gg.zue.yadef.DeferredRenderer;
/** 
 * Created by MiZu on 19.05.2015.
 */
class YadefTest extends SimpleApplication {
	package Geometry cube

	override void simpleInitApp() {
		flyCam.setMoveSpeed(100f)
		cube = new Geometry("Cube", new Box(4, 4, 4))
		cam.setFrustumFar(10000)
		initDeferred() // initRegular();
		addAmbientLight()
		addDirectionalLights()
		addPointLights(200)
		addSpotLights(200) // addFPSFLashLight();
		addSphereGrid()
		var Material material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md")
		material.setTexture("DiffuseMap", assetManager.loadTexture("Textures/diffuse.jpg"))
		material.setTexture("NormalMap", assetManager.loadTexture("Textures/normal.jpg"))
		var Geometry clone = cube.clone()
		clone.setMaterial(material)
		clone.setQueueBucket(RenderQueue.Bucket.Translucent)
		rootNode.attachChild(clone)
		clone.move(0, 20, 0)
	}

	override void simpleUpdate(float tpf) {
		super.simpleUpdate(tpf)
		moveFlashLight()
	}

	SpotLight flashLight

	def private void addFPSFLashLight() {
		flashLight = new SpotLight()
		flashLight.setSpotInnerAngle(6 * FastMath.DEG_TO_RAD)
		flashLight.setSpotOuterAngle(10 * FastMath.DEG_TO_RAD)
		flashLight.setColor(ColorRGBA.White)
		flashLight.setSpotRange(300f)
		rootNode.addLight(flashLight)
	}

	def private void moveFlashLight() {
		if (flashLight !== null) {
			flashLight.setDirection(cam.getDirection())
			flashLight.setPosition(cam.getLocation().add(flashLight.getDirection().mult(5)))
		}

	}

	def private void addSpotLights(int count) {

		for (var int i = 0; i < count; i++) {
			var SpotLight pointLight = new SpotLight()
			pointLight.setColor(ColorRGBA.randomColor().mult(2))
			pointLight.setDirection(
				new Vector3f(FastMath.nextRandomFloat() * -1, FastMath.nextRandomFloat() * -1,
					FastMath.nextRandomFloat() * -1).normalize())
			pointLight.setPosition(
				new Vector3f(FastMath.nextRandomFloat() * 12 * 20, 50, FastMath.nextRandomFloat() * 12 * 20))
			var float v = FastMath.abs(FastMath.nextRandomFloat() * 20) + 4
			System.out.println(v)
			v = 20
			var float v1 = 15
			// Math.max(v - 4, 1;
			pointLight.setSpotOuterAngle(v * FastMath.DEG_TO_RAD)
			pointLight.setSpotInnerAngle(v1 * FastMath.DEG_TO_RAD)
			rootNode.addLight(pointLight)
		}

	}

	def private void addPointLights(int count) {

		for (var int i = 0; i < count; i++) {
			var PointLight pointLight = new PointLight()
			pointLight.setColor(ColorRGBA.randomColor().mult(2))
			pointLight.setRadius(FastMath.nextRandomFloat() * 30)
			pointLight.setPosition(
				new Vector3f(FastMath.nextRandomFloat() * 12 * 20, 10, FastMath.nextRandomFloat() * 12 * 20))
			rootNode.addLight(pointLight)
		}

	}

	def package void addAmbientLight() {
		var AmbientLight ambientLight = new AmbientLight()
		ambientLight.setColor(ColorRGBA.White.mult(0.1f))
		rootNode.addLight(ambientLight)
	}

	def package void addDirectionalLights() {
		var DirectionalLight directionalLight = new DirectionalLight()
		directionalLight.setDirection(new Vector3f(-1, 0.5f, 0).normalize())
		directionalLight.setColor(ColorRGBA.White.mult(0.5f))
		rootNode.addLight(directionalLight)
	}

	def package void initDeferred() {
		for (SceneProcessor sceneProcessor : viewPort.getProcessors()) {
			viewPort.removeProcessor(sceneProcessor)
		}
		// viewPort.addProcessor(new DeferredRenderer(this));
		val MatIdManager matIdManager = new MatIdManager()
		var SceneProcessor4Deferred out = new SceneProcessor4Deferred(this.getAssetManager(), matIdManager)
		this.getViewPort().addProcessor(out)
		this.getStateManager().attach(
			new AppState4ViewDeferredTexture(out, AppState4ViewDeferredTexture.ViewKey.values()))
		var Material material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md")
		material.setTexture("DiffuseMap", assetManager.loadTexture("Textures/diffuse.jpg"))
		material.setTexture("NormalMap", assetManager.loadTexture("Textures/normal.jpg"))
		cube.setMaterial(material)
		cube.getMaterial().setColor("Diffuse", new ColorRGBA(0.5f, 0.5f, 0.5f, 0f))
		var MaterialConverter mc = new MaterialConverter(assetManager, matIdManager)
		cube.breadthFirstTraversal(mc) // Material material = new Material(assetManager, "Materials/yadef/Deferred/Deferred.j3md");
		// material.setTexture("diffuseTexture", assetManager.loadTexture("Textures/diffuse.jpg"));
		// material.setTexture("normalTexture", assetManager.loadTexture("Textures/normal.jpg"));
		// cube.setMaterial(material);
	}

	def package void initRegular() {
		var Material material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md")
		material.setTexture("DiffuseMap", assetManager.loadTexture("Textures/diffuse.jpg"))
		material.setTexture("NormalMap", assetManager.loadTexture("Textures/normal.jpg"))
		cube.setMaterial(material)
		cube.getMaterial().setColor("Diffuse", new ColorRGBA(0.5f, 0.5f, 0.5f, 0f))
	}

	def package void addSingleSphere() {
		rootNode.attachChild(cube)
	}

	def package void addSphereGrid() {
		var SimpleBatchNode simpleBatchNode = new SimpleBatchNode()

		for (var int x = 0; x < 20; x++) {

			for (var int y = 0; y < 20; y++) {
				var Geometry clone = cube.clone()
				clone.setLocalTranslation(x * 12, FastMath.nextRandomFloat() * 10, y * 12)
				simpleBatchNode.attachChild(clone)
			}

		}
		simpleBatchNode.batch()
		rootNode.attachChild(simpleBatchNode)
		simpleBatchNode.updateGeometricState()
	}

	def static void main(String[] args) {
		var AppSettings settings = new AppSettings(true)
		settings.setResolution(1280, 720)
		settings.setVSync(false)
		settings.setFullscreen(false)
		settings.setDepthBits(24) // settings.setStencilBits(8);
		settings.setRenderer(AppSettings.LWJGL_OPENGL3)
		var YadefTest app = new YadefTest()
		app.setSettings(settings)
		app.start()
	}

}
