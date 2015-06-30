package sandbox

import jme3_ext_deferred.MatIdManager
import jme3_ext_deferred.MaterialConverter
import com.jme3.app.Application
import com.jme3.app.SimpleApplication
import com.jme3.app.state.AbstractAppState
import com.jme3.app.state.AppStateManager
import com.jme3.asset.AssetManager
import com.jme3.bounding.BoundingBox
import com.jme3.light.AmbientLight
import com.jme3.light.DirectionalLight
import com.jme3.light.SpotLight
import com.jme3.material.Material
import com.jme3.material.MaterialCustom
import com.jme3.math.ColorRGBA
import com.jme3.math.FastMath
import com.jme3.math.Vector3f
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.renderer.queue.RenderQueue.ShadowMode
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.scene.control.AbstractControl
import com.jme3.scene.shape.Box
import com.jme3.scene.shape.Sphere
import com.jme3.util.SkyFactory
import com.jme3.util.SkyFactory.EnvMapType
import org.eclipse.xtend.lib.annotations.FinalFieldsConstructor

@FinalFieldsConstructor
class AppState4Sample03 extends AbstractAppState {
	final ColorRGBA[] colors = (#[ColorRGBA::Red, ColorRGBA::Green, ColorRGBA::Blue, ColorRGBA::White, ColorRGBA::Cyan,
		ColorRGBA::DarkGray, ColorRGBA::Magenta, ColorRGBA::Orange, ColorRGBA::Pink, ColorRGBA::Yellow] as ColorRGBA[])
	final public MatIdManager matIdManager
	package AssetManager assetManager

	override void initialize(AppStateManager stateManager, Application app) {
		assetManager = app.getAssetManager()
		var Node anchor = new Node("Sample01")
		makeScene(anchor, 5, 7, 4) // anchor.attachChild(SkyFactory.createSky(assetManager, "Textures/Sky/Bright/BrightSky.dds", EnvMapType.SphereMap));
		makeLigths(anchor)
		(app as SimpleApplication).getRootNode().attachChild(anchor)
	}

	def package Spatial makeScene(Node anchor0, int nbX, int nbY, int nbZ) {
		var Material matDef = new MaterialCustom(assetManager, "MatDefs/deferred/gbuffer.j3md")
		matDef.setInt("MatId", matIdManager.findMatId(ColorRGBA::Gray, ColorRGBA::White))
		var Geometry[] geotmpl = (#[assetManager.loadModel("Models/teapot.j3o") as Geometry,
			new Geometry("box", new Box(0.5f, 0.5f, 0.5f)),
			new Geometry("sphere", new Sphere(16, 16, 0.5f))] as Geometry[])
			var Vector3f margin = new Vector3f(0.1f, 0.1f, 0.1f)
			var Vector3f cellSizeMax = new Vector3f()
			for (Spatial child : geotmpl) {
				var BoundingBox bb = child.getWorldBound() as BoundingBox
				cellSizeMax.maxLocal(bb.getExtent(null).multLocal(2.0f))
			}
			cellSizeMax.addLocal(margin)
			var Node group = new Node("group")

			for (var int x = 0; x < nbX; x++) {

				for (var int y = 0; y < nbY; y++) {

					for (var int z = 0; z < nbZ; z++) {
						var int i = z + y * nbZ + x * nbZ * nbY
						var Spatial child = {
							val _rdIndx_geotmpl = i % geotmpl.length
							geotmpl.get(_rdIndx_geotmpl)
						}.clone()
						var Vector3f pos = new Vector3f(cellSizeMax).multLocal(x, y, z)
						child.center()
						pos.addLocal(child.getLocalTranslation())
						child.setLocalTranslation(pos)
						var Material mat = matDef.clone()
						mat.setInt("MatId", matIdManager.findMatId({
							val _rdIndx_colors = i % colors.length
							colors.get(_rdIndx_colors)
						}, ColorRGBA::White))
						mat.setColor("Color", {
							val _rdIndx_colors = i % colors.length
							colors.get(_rdIndx_colors)
						})
						mat.setColor("Specular", ColorRGBA::White)
						child.setMaterial(mat)
						child.setShadowMode(ShadowMode::CastAndReceive)
						group.attachChild(child)
					}

				}

			}
			group.center()
			var Spatial sponza = assetManager.loadModel("Models/crytek_sponza2.j3o")
			sponza.scale(10.0f)
			sponza.setShadowMode(ShadowMode::CastAndReceive) // sponza.setLocalTranslation(new Vector3f(-8.f, -0.25f, 0.f).multLocal(sponza.getWorldBound().getCenter()));
			sponza.setLocalTranslation(new Vector3f(0f, -8f + 1.5f, 0f)) // -8 if the location of physical floor in Sample02 :-P
			group.attachChild(sponza)
			var MaterialConverter mc = new MaterialConverter(assetManager, matIdManager)
			group.breadthFirstTraversal(mc)
			anchor0.attachChild(group)
			return group
		}

		def package void makeLigths(Node anchor) {
			val light0 = new AmbientLight()
			light0.setColor(new ColorRGBA(0.1f, 0.1f, 0.04f, 1.0f))
			anchor.addLight(light0) // ColorRGBA light1c = new ColorRGBA(241f/255f*0.2f,215f/255f*0.2f,106f/255f*0.2f,1.0f);
			val light1c = new ColorRGBA(0.8f, 0.8f, 0.8f, 1.0f)
			val light1 = new DirectionalLight()
			light1.setColor(light1c)
			light1.setDirection(new Vector3f(-0.5f, -5f, -0.0f).normalizeLocal())
			light1.setName("ldir")
			anchor.addLight(light1)
			anchor.addControl(new AbstractControl() {
				float time = 0

				override protected void controlUpdate(float tpf) {
					time += tpf
					var float angle = 2 * FastMath::PI * ((time % 20f) / 20f)
					light1.setDirection(new Vector3f(0, FastMath::sin(angle), FastMath::cos(angle)).normalizeLocal())
				}

				override protected void controlRender(RenderManager rm, ViewPort vp) {
				}
			})
			anchor.addControl(new AbstractControl() {
				SpotLight[] pls = newArrayOfSize(4)
				var Node anchor0 = null
				float radius = 10f
				float rangeY = 30

				override void setSpatial(Spatial spatial) {
					super.setSpatial(spatial)
					if (anchor0 !== null && anchor0 !== spatial) {

						for (var int i = 0; i < pls.length; i++) {
							anchor0.removeLight({
								val _rdIndx_pls = i
								pls.get(_rdIndx_pls)
							})
						}
						anchor0 = null
					}
					if (spatial !== null && anchor0 !== spatial) {
						anchor0 = spatial as Node
						var float spotOuterAngle = FastMath::PI / 4f
						// FastMath.atan(radius/rangeY);
						System::out.println('''spotOuterAngle : «spotOuterAngle»'''.toString)
						for (var int i = 0; i < pls.length; i++) {
							var SpotLight pl = new SpotLight()
							pl.setName('''pl«i»'''.toString) // pl.setDirection(Vector3f.UNIT_Y);
							pl.setColor({
								val _rdIndx_colors = i % colors.length
								colors.get(_rdIndx_colors)
							})
							pl.setSpotRange(rangeY)
							pl.setSpotOuterAngle(spotOuterAngle)
							pl.setSpotInnerAngle(spotOuterAngle * 0.8f) // pl.setDirection(direction);
							anchor0.addLight(pl)
							{
								val _wrVal_pls = pls
								val _wrIndx_pls = i
								_wrVal_pls.set(_wrIndx_pls, pl)
							}
						}

					}

				}

				override protected void controlUpdate(float tpf) {
					// helix x spiral ?
					// float deltaItem = (float)(2f * Math.PI / pls.length);
					// float deltaTime = 0;//(float)Math.PI * (timer.getTimeInSeconds() % 6) / 3; // 3s for full loop
					// for (int i = 0; i < pls.length; i++){
					// Geometry pl = pls[i];
					// float angle = deltaItem * i + deltaTime;
					// float d = radius*1.5f;
					// pl.setLocalTranslation(FastMath.cos(angle) * d, -rangeY, FastMath.sin(angle) * d);
					// }
					//
					// grid ?
					var int nbSize = Math::ceil(Math::sqrt(pls.length as double)) as int

					for (var int x = 0; x < nbSize; x++) {

						for (var int z = 0; z < nbSize; z++) {
							var int i = x + z * nbSize
							if (i < pls.length) {
								var SpotLight pl = {
									val _rdIndx_pls = i
									pls.get(_rdIndx_pls)
								}
								var Vector3f v = pl.getPosition()
								v.set((x - nbSize / 2) * 2f * radius, rangeY * 0.5f, (z - nbSize / 2) * 2f * radius)
								pl.setPosition(v)
							}

						}

					}

				}

				override protected void controlRender(RenderManager rm, ViewPort vp) {
				}
			})
		}

	}
	