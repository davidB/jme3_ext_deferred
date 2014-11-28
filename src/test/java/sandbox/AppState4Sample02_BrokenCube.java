package sandbox;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import jme3_ext_deferred.Helpers4Lights;
import jme3_ext_deferred.MatIdManager;
import jme3_ext_deferred.MaterialConverter;
import lombok.RequiredArgsConstructor;
import rx_ext.Observable4AddRemove;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.PlaneCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Plane;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Quad;

/**
 * @from dmonkey (by kwando)
 */
@RequiredArgsConstructor
public class AppState4Sample02_BrokenCube extends AbstractAppState {

	private BatchNode cubesNode;
	private float BACKGROUND_INTENSITY = 1.5f;
	public float LIGHT_SIZE = 1.5f;

	final public MatIdManager matIdManager;
	final public Observable4AddRemove<Geometry> lights;


	Node rootNode;
	AssetManager assetManager;
	AppStateManager stateManager;
	Application app;

	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		this.app = app;
		assetManager = app.getAssetManager();
		rootNode = new Node("Sample02");
		((SimpleApplication)app).getRootNode().attachChild(rootNode);
		this.stateManager = stateManager;
		BulletAppState bullet = new BulletAppState();
		bullet.setThreadingType(BulletAppState.ThreadingType.PARALLEL);

		stateManager.attach(bullet);
//		bullet.getPhysicsSpace().addCollisionListener(new PhysicsCollisionListener() {
//			private AtomicLong collisionCount = new AtomicLong();
//			@Override
//			public void collision(PhysicsCollisionEvent event) {
//				long collisions = collisionCount.incrementAndGet();
//				if (collisions % 1000 == 0) {
//					System.out.printf("%dK collisions\n", collisions / 1000);
//				}
//			}
//		});

		PlaneCollisionShape plane = new PlaneCollisionShape(new Plane(Vector3f.UNIT_Y, -10));
		PhysicsRigidBody body = new PhysicsRigidBody(plane);
		body.setMass(0);
		body.setRestitution(1);
		bullet.getPhysicsSpace().add(body);

		ColorRGBA c = new ColorRGBA(0.19136488f, 0.5587857f, 0.60471356f, 1f);
		c.multLocal(BACKGROUND_INTENSITY);
		addPointLight(20f, c, new Vector3f(0, -3, 0));
		addPointLight(20f, c, new Vector3f(3, -15, 3));
		addPointLight(5f, ColorRGBA.Cyan.clone(), new Vector3f(0, 3, 0));

		for (int i = 0; i < 10; i++) {
			randomizeLight();
		}


//		Material mat = assetManager.loadMaterial("DMonkey/TestMaterial.j3m");
//		TextureTools.setAnistropic(mat, "DiffuseTex", 8);
		final Spatial model = assetManager.loadModel("Models/broken_cube.j3o");
//		model.setMaterial(mat);
		Random random = new Random(7);
		cubesNode = new BatchNode();
		for (int i = 0; i < 50; i++) {
			Vector3f randomPos = new Vector3f(random.nextFloat() * 10, random.nextFloat() * 10, random.nextFloat() * 10);
			model.setLocalTranslation(randomPos.subtractLocal(5, 5, 5));
			Spatial geom = model.clone();
			//geom.addControl(new RotationControl(new Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())));
			cubesNode.attachChild(geom);
			BoxCollisionShape box = new BoxCollisionShape(new Vector3f(.5f, .5f, .5f));
			RigidBodyControl control = new RigidBodyControl(box, 1);
			geom.addControl(control);
			control.setMass(120);
			control.setLinearSleepingThreshold(10f);
			control.setLinearDamping(0.4f);
			bullet.getPhysicsSpace().add(control);
		}
		MaterialConverter mc = new MaterialConverter(assetManager, matIdManager);
		cubesNode.breadthFirstTraversal(mc);
		rootNode.attachChild(cubesNode);
		/*
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

		setupControls(app.getInputManager());
	}

	private void randomizeLight() {
		ColorRGBA color = ColorRGBA.randomColor();
		color.multLocal(.5f);
		Vector3f pos = new Vector3f(FastMath.nextRandomFloat() * 10 - 5, FastMath.nextRandomFloat() * 8 - 10, FastMath.nextRandomFloat() * 10 - 5);
		addPointLight(8, color, pos);
	}

	private void addPointLight(float radius, ColorRGBA color, Vector3f pos) {
		Geometry pointLight = Helpers4Lights.newPointLight("envlight", radius, color, assetManager);
		pointLight.setLocalTranslation(pos);
		rootNode.attachChild(pointLight);
		lights.add.onNext(pointLight);
	}


	@Override
	public void update(float tpf) {
		super.update(tpf);
		lastFire += tpf;

		if (isFiring) {
			for (int i = 0; i < ballsPerFrame; i++) {
				addCanonBall();
			}
			lastFire = 0;
		}
	}

	private Queue<Spatial> freeBalls = new LinkedList<Spatial>();
	private float lastFire = 0;
	private float ballsPerSec = 300;
	private int maxBalls = 500;
	private int activeBalls = 0;
	private int ballsPerFrame = 10;
	private float maxLife = 6;
	private boolean isFiring = false;
	private CollisionShape shape = new SphereCollisionShape(0.075f);

	private void addCanonBall() {
		//Camera cam = app.getCamera();
		Camera cam = app.getViewPort().getCamera();
		if (lastFire < 1f / ballsPerSec || activeBalls >= maxBalls) {
			return;
		}

		activeBalls++;
		Spatial ball;
		if (!freeBalls.isEmpty()) {
			ball = freeBalls.poll();
		} else {
			float size = 0.15f;
			ColorRGBA color = ColorRGBA.randomColor();
			Node n = new Node("projectile");

			Geometry geom = new Geometry("particle", new Quad(size, size));
			geom.setLocalTranslation(-0.5f * size, 0.5f * -size, 0.0f);
			Material lightMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
			lightMaterial.setColor("Color", color);
			lightMaterial.setTexture("LightMap", assetManager.loadTexture("Textures/particletexture.jpg"));
			lightMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Additive);
			lightMaterial.getAdditionalRenderState().setDepthWrite(false);
			geom.setMaterial(lightMaterial);
			geom.setQueueBucket(Bucket.Transparent);
			BillboardControl billboarder = new BillboardControl();
			//billboarder.setAlignment(BillboardControl.Alignment.Camera);
//			geom.addControl(billboarder);
//			n.attachChild(geom);
			Node anchor0 = new Node();
			anchor0.addControl(billboarder);
			anchor0.attachChild(geom);
			n.attachChild(anchor0);

			//pointLight.setColor(ColorRGBA.randomColor().multLocal(0.1f));
			Geometry pointLight = Helpers4Lights.newPointLight("light", LIGHT_SIZE, color, assetManager);
			pointLight.center();
			n.attachChild(pointLight);
			//dsp.addLight(pointLight, true);
			lights.add.onNext(pointLight);
			n.center();
			ball = n;
		}
		ball.addControl(new AbstractControl() {
			private float time = 0;

			@Override
			protected void controlUpdate(float tpf) {
				time += tpf;
				if (time > maxLife) {
					Spatial spat = spatial;
					freeBalls.add(spat);
					spat.removeFromParent();
					spat.removeControl(this);
					RigidBodyControl control = spat.getControl(RigidBodyControl.class);
					if (control != null) {
						stateManager.getState(BulletAppState.class).getPhysicsSpace().remove(control);
						//control.setEnabled(false);
						spat.removeControl(control);
					}
					activeBalls--;

//					LightControl lc = spat.getControl(LightControl.class);
//					if (lc != null) {
//						lc.getLight().setColor(ColorRGBA.Black);
//					}
					Geometry l = (Geometry)((Node)spat).getChild("light");
					if (l != null) {
						//l.getMaterial().setColor("Color", ColorRGBA.Black);
						Helpers4Lights.setEnabled(l, false);
					}
				}
			}

			@Override
			protected void controlRender(RenderManager rm, ViewPort vp) {
			}
		});
		float weight = 0.1f;
		RigidBodyControl control = new RigidBodyControl(shape, weight);
		ball.addControl(control);
		stateManager.getState(BulletAppState.class).getPhysicsSpace().add(control);
		control.setPhysicsLocation(cam.getLocation().add(cam.getDirection().mult(2)));
		control.applyImpulse(cam.getDirection().normalize().mult(2), Vector3f.ZERO);
		control.setFriction(1f);
		control.setRestitution(0.56f);
		control.setAngularDamping(.67f);
//		geom.getControl(LightControl.class).getLight().setColor(rgba);
//		geom.getMaterial().setColor("Color", rgba);
		Helpers4Lights.setEnabled((Geometry)((Node)ball).getChild("light"), true);

		rootNode.attachChild(ball);
	}

	private void setupControls(InputManager inputManager) {
		inputManager.addMapping("SHOOT", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
		inputManager.addListener(new ActionListener() {
			@Override
			public void onAction(String name, boolean isPressed, float tpf) {
				isFiring = isPressed;
			}
		}, "SHOOT");

		inputManager.addMapping("DEBUG_PHYSICS", new KeyTrigger(KeyInput.KEY_F2));
		inputManager.addListener(new ActionListener() {

			@Override
			public void onAction(String name, boolean isPressed, float tpf) {
				if (isPressed) {
					BulletAppState bullet = app.getStateManager().getState(BulletAppState.class);
					bullet.setDebugEnabled(!bullet.isDebugEnabled());
				}
			}

		}, "DEBUG_PHYSICS");
//		inputManager.addMapping("BATCH_CUBES", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
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
