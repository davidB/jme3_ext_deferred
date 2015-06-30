package sandbox

import com.jme3.app.Application
import com.jme3.app.SimpleApplication
import com.jme3.app.state.AbstractAppState
import com.jme3.app.state.AppStateManager
import com.jme3.asset.AssetManager
import com.jme3.bullet.BulletAppState
import com.jme3.bullet.collision.shapes.BoxCollisionShape
import com.jme3.bullet.collision.shapes.CollisionShape
import com.jme3.bullet.collision.shapes.PlaneCollisionShape
import com.jme3.bullet.collision.shapes.SphereCollisionShape
import com.jme3.bullet.control.RigidBodyControl
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.input.InputManager
import com.jme3.input.KeyInput
import com.jme3.input.MouseInput
import com.jme3.input.controls.ActionListener
import com.jme3.input.controls.KeyTrigger
import com.jme3.input.controls.MouseButtonTrigger
import com.jme3.light.PointLight
import com.jme3.material.Material
import com.jme3.material.RenderState
import com.jme3.math.ColorRGBA
import com.jme3.math.FastMath
import com.jme3.math.Plane
import com.jme3.math.Vector3f
import com.jme3.renderer.Camera
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.renderer.queue.RenderQueue.Bucket
import com.jme3.scene.BatchNode
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.scene.control.AbstractControl
import com.jme3.scene.control.BillboardControl
import com.jme3.scene.control.LightControl
import com.jme3.scene.shape.Box
import com.jme3.scene.shape.Quad
import java.util.LinkedList
import java.util.Queue
import java.util.Random
import jme3_ext_deferred.MatIdManager
import jme3_ext_deferred.MaterialConverter
import org.eclipse.xtend.lib.annotations.FinalFieldsConstructor

/** 
 * @from dmonkey (by kwando)
 */
@FinalFieldsConstructor
class AppState4Sample04_BrokenCube extends AbstractAppState {
	BatchNode cubesNode
	float BACKGROUND_INTENSITY = 1.5f
	public float LIGHT_SIZE = 1.5f
	final public MatIdManager matIdManager
	package Node rootNode
	package AssetManager assetManager
	package AppStateManager stateManager
	package Application app

	override void initialize(AppStateManager stateManager, Application app) {
		this.app=app assetManager=app.getAssetManager() rootNode=new Node("Sample02") (app as SimpleApplication).getRootNode().attachChild(rootNode) this.stateManager=stateManager var BulletAppState bullet=new BulletAppState() 
		bullet.setThreadingType(BulletAppState.ThreadingType::PARALLEL) stateManager.attach(bullet) //		bullet.getPhysicsSpace().addCollisionListener(new PhysicsCollisionListener() {
		//			private AtomicLong collisionCount = new AtomicLong();
		//			@Override
		//			public void collision(PhysicsCollisionEvent event) {
		//				long collisions = collisionCount.incrementAndGet();
		//				if (collisions % 1000 == 0) {
		//					System.out.printf("%dK collisions\n", collisions / 1000);
		//				}
		//			}
		//		});
		var PlaneCollisionShape plane=new PlaneCollisionShape(new Plane(Vector3f::UNIT_Y,-10)) 
		var PhysicsRigidBody body=new PhysicsRigidBody(plane) 
		body.setMass(0) body.setRestitution(1) bullet.getPhysicsSpace().add(body) var ColorRGBA c=new ColorRGBA(0.19136488f,0.5587857f,0.60471356f,1f) 
		c.multLocal(BACKGROUND_INTENSITY)
		addPointLight(20f, c, new Vector3f(0,-3,0))
		addPointLight(20f, c, new Vector3f(3,-15,3))
		addPointLight(5f, ColorRGBA::Cyan.clone(), new Vector3f(0,3,0)) 
		for (var int i=0 ; i < 10; i++) {
			randomizeLight() 
		}
		//		TextureTools.setAnistropic(mat, "DiffuseTex", 8);
		val Spatial model=new Geometry("bcube",new Box(0.5f,0.5f,0.5f)) 
		var Material mat=new Material(assetManager,"Common/MatDefs/Light/Lighting.j3md") 
		mat.setColor("Diffuse", ColorRGBA::Pink) model.setMaterial(mat) //final Spatial model = assetManager.loadModel("Models/broken_cube.j3o");
		var Random random=new Random(7) 
		cubesNode=new BatchNode() 
		for (var int i=0 ; i < 50; i++) {
			var Vector3f randomPos=new Vector3f(random.nextFloat() * 10,random.nextFloat() * 10,random.nextFloat() * 10) 
			model.setLocalTranslation(randomPos.subtractLocal(5, 5, 5)) var Spatial geom=model.clone() 
			//geom.addControl(new RotationControl(new Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())));
			cubesNode.attachChild(geom) var BoxCollisionShape box=new BoxCollisionShape(new Vector3f(0.5f, 0.5f, 0.5f))

			var RigidBodyControl control = new RigidBodyControl(box, 1)
			geom.addControl(control)
			control.setMass(120)
			control.setLinearSleepingThreshold(10f)
			control.setLinearDamping(0.4f)
			bullet.getPhysicsSpace().add(control) 
		}
		var MaterialConverter mc=new MaterialConverter(assetManager,matIdManager) 
		cubesNode.breadthFirstTraversal(mc) rootNode.attachChild(cubesNode) /*
     Spatial geom = model.clone();
     geom.scale(2);

     geom.setLocalTranslation(Vector3f.ZERO);
     geom.setMaterial(assetManager.loadMaterial("Materials/Transparent.j3m"));
     rootNode.attachChild(geom);

     /* AmbientLight al = new AmbientLight();
     al.setColor(ColorRGBA.Cyan.mult(.15f));
     rootNode.addLight(al);


     DirectionalLight dl = new DirectionalLight();
     al.setColor(ColorRGBA.Blue.mult(.3f));
     dl.setDirection(Vector3f.UNIT_XYZ.mult(-1));
     rootNode.addLight(dl);
		 * */
		//stateManager.attach(new DebugControl(dsp));
		//DeferredShadingUtils.scanNode(dsp, rootNode);
		setupControls(app.getInputManager()) 
	}
	def randomizeLight() {
		var color = ColorRGBA::randomColor() 
		color.multLocal(0.5f)
		val pos=new Vector3f(FastMath::nextRandomFloat() * 10 - 5,FastMath::nextRandomFloat() * 8 - 10,FastMath::nextRandomFloat() * 10 - 5) 
		addPointLight(8, color, pos) 
	}
	def addPointLight(float radius, ColorRGBA color, Vector3f pos) {
		var PointLight pl=new PointLight() 
		pl.setRadius(radius) pl.setColor(color) pl.setPosition(pos) rootNode.addLight(pl) 
	}
	override void update(float tpf) {
		super.update(tpf) if (isFiring) {
			addCanonBall() 
		}
		
	}
	Queue<Spatial> freeBalls=new LinkedList<Spatial>()
	boolean isFiring=false
	CollisionShape shape=new SphereCollisionShape(0.075f)
	
	def addCanonBall() {
		//Camera cam = app.getCamera();
		var Camera cam=app.getViewPort().getCamera() 
		var Spatial ball 
		if (!freeBalls.isEmpty()) {
			ball=freeBalls.poll() 
		} else {
			var float size=0.15f 
			var ColorRGBA color=ColorRGBA::randomColor() 
			var Node n=new Node("projectile") 
			var Geometry geom=new Geometry("particle",new Quad(size,size)) 
			geom.setLocalTranslation(-0.5f * size, -0.5f * size, 0.0f)
			val lightMaterial= new Material(assetManager,"Common/MatDefs/Misc/UnshadedNodes.j3md") 
			lightMaterial.setColor("Color", color)
			lightMaterial.setTexture("LightMap", assetManager.loadTexture("Textures/particletexture.jpg"))
			lightMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode::Additive)
			lightMaterial.getAdditionalRenderState().setDepthWrite(false)
			geom.setMaterial(lightMaterial)
			geom.setQueueBucket(Bucket::Transparent)
			 var BillboardControl billboarder=new BillboardControl() 
			//			geom.addControl(billboarder);
			//billboarder.setAlignment(BillboardControl.Alignment.Camera);
			//			geom.addControl(billboarder);
			var Node anchor0=new Node() 
			anchor0.addControl(billboarder) anchor0.attachChild(geom) n.attachChild(anchor0) var float weight=0.1f 
			var RigidBodyControl physic=new RigidBodyControl(shape,weight) 
			stateManager.getState(typeof(BulletAppState)).getPhysicsSpace().add(physic) n.addControl(physic) var PointLight pl=new PointLight() 
			pl.setColor(color) pl.setRadius(LIGHT_SIZE) n.addControl(new LightControl(pl)) var BallLightControl blc=new BallLightControl(rootNode,cam,freeBalls) 
			n.addControl(blc) blc.setEnabled(false) //n.center();
			ball=n 
		}var BallLightControl blc=ball.getControl(typeof(BallLightControl)) 
		blc.setEnabled(true) 
	}
	def setupControls(InputManager inputManager) {
		inputManager.addMapping("SHOOT", new MouseButtonTrigger(MouseInput::BUTTON_LEFT)) inputManager.addListener(([String name, boolean isPressed, float tpf|isFiring=isPressed ] as ActionListener), "SHOOT") inputManager.addMapping("DEBUG_PHYSICS", new KeyTrigger(KeyInput::KEY_F2)) inputManager.addListener(([String name, boolean isPressed, float tpf|if (isPressed) {
			var BulletAppState bullet=app.getStateManager().getState(typeof(BulletAppState)) 
			bullet.setDebugEnabled(!bullet.isDebugEnabled()) 
		}
		] as ActionListener), "DEBUG_PHYSICS") //		inputManager.addMapping("BATCH_CUBES", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
		//		inputManager.addListener(new ActionListener() {
		//			@Override
		//			public void onAction(String name, boolean isPressed, float tpf) {
		//				if (!isPressed) {
		//					System.out.println("BATCHING CUBES");
		//					rootNode.breadthFirstTraversal(new SceneGraphVisitor() {
		//						@Override
		//						public void visit(Spatial spatial) {
		//							RigidBodyControl control = spatial.getControl(RigidBodyControl.class);
		//							if (control != null) {
		//								spatial.removeControl(control);
		//								stateManager.getState(BulletAppState.class).getPhysicsSpace().remove(control);
		//							}
		//						}
		//					});
		//
		//
		//
		//					//cubesNode.addControl(control);
		//					//control.setKinematic(true);
		//					//stateManager.getState(BulletAppState.class).getPhysicsSpace().add(control);
		//				}
		//			}
		//		}, "BATCH_CUBES");
		
	}
}

@FinalFieldsConstructor
package class BallLightControl extends AbstractControl {
	float time = 0
	public final Node rootNode
	public final Camera cam
	public final Queue<Spatial> freeBalls
	public float maxLife = 6

	override protected void controlUpdate(float tpf) {
		time += tpf
		if (time > maxLife) {
			setEnabled(false)
		}

	}

	override void setEnabled(boolean v) {
		if(isEnabled() === v) return;
		super.setEnabled(v)
		if (isEnabled()) {
			onEnable()
		} else {
			onDisable()
		}
	}

	def onEnable() {
		var RigidBodyControl physic = spatial.getControl(typeof(RigidBodyControl))
		if (physic !==	null){
			physic.setEnabled(true) physic.setPhysicsLocation(cam.getLocation().add(cam.getDirection().mult(2))) physic.applyImpulse(cam.getDirection().normalize().mult(2), Vector3f::ZERO) physic.setFriction(1f) physic.setRestitution(0.56f) physic.setAngularDamping(0.67f) 
		}

		var LightControl lightc = spatial.getControl(typeof(LightControl))
		if (lightc !== null) {
			lightc.setEnabled(true) rootNode.addLight(lightc.getLight()) 
		}
		rootNode.attachChild(spatial) time=0 
	}
	def onDisable() {
		freeBalls.add(spatial) spatial.removeFromParent() var RigidBodyControl physic=spatial.getControl(typeof(RigidBodyControl)) 
		if (physic !== null) {
			physic.setEnabled(false) 
		}
		var LightControl lightc=spatial.getControl(typeof(LightControl)) 
		if (lightc !== null) {
			lightc.setEnabled(false) rootNode.removeLight(lightc.getLight()) 
		}
		
	}
	override controlRender(RenderManager rm, ViewPort vp) {
		
	}
		
}
	