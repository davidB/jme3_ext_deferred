package jme3_ext_deferred;

import java.util.ArrayList;
import java.util.Collection;

import com.jme3.asset.AssetManager;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.material.MaterialCustom;
import com.jme3.material.RenderState;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.material.RenderState.StencilOperation;
import com.jme3.material.RenderState.TestFunction;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.shadow.CompareMode;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.SpotLightShadowRenderer;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.FrameBufferHack;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;


// @see http://ogldev.atspace.co.uk/www/tutorial37/tutorial37.html
class Pass4LBuffer {
	public final TBuffer lbuffer;

	/** for debug : display light geometry in wireframe */
	public boolean showLightGeom = false;
	final Material debugGeomMat;

	final GBuffer gbuffer;
	final ViewPort vp;
	final RenderManager rm;
	final Vector4f m_ProjInfo;
	final Vector3f m_ClipInfo;
	final Texture2D m_MatBuffer;

	final RenderState rsLBufMask = new RenderState();
	final RenderState rsLBuf = new RenderState();
	final RenderState rs0 = new RenderState();
	final RenderState rsAmbiant = new RenderState();
	final RenderState rsShadow = new RenderState();
	protected AmbientLight ambient0;
	final Collection<Light> lights0 = new ArrayList<Light>();

	final AssetManager assetManager;

	final LigthPeerTmpl4Ambient ligthPeerTmpl4Ambient;
	final LigthPeerTmpl4Spot ligthPeerTmpl4Spot;
	final LigthPeerTmpl4Spot ligthPeerTmpl4SpotAndShadow;
	final LigthPeerTmpl4Point ligthPeerTmpl4Point;
	final LigthPeerTmpl4Directional ligthPeerTmpl4Directional;
	final LigthPeerTmpl4Directional ligthPeerTmpl4DirectionalAndShadow;

	public Pass4LBuffer(int width, int height, ViewPort vp, RenderManager rm, AssetManager assetManager, GBuffer gbuffer, Texture2D matBuffer) {
		this.gbuffer = gbuffer;
		this.lbuffer = new TBuffer(width, height, Format.RGB16F);
		//this.lbuffer = new TBuffer(width, height, Format.RGB8);
		lbuffer.fb.setDepthTexture(gbuffer.depth);
		FrameBufferHack.setDepthStencilAttachment(lbuffer.fb);
		//FrameBufferHack.setStencilAttachment(lbuffer.fb);

		this.vp = vp;
		this.rm = rm;
		this.assetManager = assetManager;
		this.m_MatBuffer = matBuffer;
		Camera cam = vp.getCamera();
		this.m_ProjInfo = Helpers.projInfo(cam, width, height);
		this.m_ClipInfo = Helpers.clipInfo(cam);
		this.debugGeomMat = assetManager.loadMaterial("Materials/deferred/debugGeom.j3m");

		rsLBufMask.setStencil(true,
				//_frontStencilStencilFailOperation, _frontStencilDepthFailOperation, _frontStencilDepthPassOperation,
				StencilOperation.Keep, StencilOperation.DecrementWrap, StencilOperation.Keep,
				//_backStencilStencilFailOperation, _backStencilDepthFailOperation, _backStencilDepthPassOperation,
				StencilOperation.Keep, StencilOperation.IncrementWrap, StencilOperation.Keep,
				//_frontStencilFunction, _backStencilFunction
				TestFunction.Always, TestFunction.Always
				);
		rsLBufMask.setDepthTest(true);
		rsLBufMask.setDepthWrite(false);
		rsLBufMask.setFaceCullMode(FaceCullMode.Off);
		rsLBufMask.setColorWrite(false);
		rsLBufMask.setBlendMode(BlendMode.Off);

		rsLBuf.setStencil(true,
				//_frontStencilStencilFailOperation, _frontStencilDepthFailOperation, _frontStencilDepthPassOperation,
				StencilOperation.Keep, StencilOperation.Keep, StencilOperation.Keep,
				//_backStencilStencilFailOperation, _backStencilDepthFailOperation, _backStencilDepthPassOperation,
				StencilOperation.Keep, StencilOperation.Keep, StencilOperation.Keep,
				//_frontStencilFunction, _backStencilFunction
				TestFunction.NotEqual, TestFunction.NotEqual
				);
		rsLBuf.setDepthTest(false);
		rsLBuf.setDepthWrite(false);
		rsLBuf.setFaceCullMode(FaceCullMode.Back);
		rsLBuf.setBlendMode(BlendMode.Additive);

		rs0.setDepthTest(false);
		rs0.setDepthWrite(false);
		rs0.setFaceCullMode(FaceCullMode.Back);
		rs0.setBlendMode(BlendMode.Additive);

		rsAmbiant.setDepthTest(false);
		rsAmbiant.setDepthWrite(false);
		rsAmbiant.setFaceCullMode(FaceCullMode.Back);
		rsAmbiant.setBlendMode(BlendMode.Additive);

		//TODO lazy creation of shadowMapGens
		//TODO allow to configure shadowMapGens (use a Provider)
		ligthPeerTmpl4Ambient = new LigthPeerTmpl4Ambient(this);
		ligthPeerTmpl4Directional = new LigthPeerTmpl4Directional(this, false);
		ligthPeerTmpl4DirectionalAndShadow = new LigthPeerTmpl4Directional(this, true);
		ligthPeerTmpl4Spot = new LigthPeerTmpl4Spot(this, false);
		ligthPeerTmpl4SpotAndShadow = new LigthPeerTmpl4Spot(this, true);
		ligthPeerTmpl4Point = new LigthPeerTmpl4Point(this, false);
	}

	void initMaterial(Material mat) {
		mat.setTexture("MatBuffer", m_MatBuffer);
		mat.setTexture("DepthBuffer", gbuffer.depth);
		mat.setTexture("NormalBuffer", gbuffer.normal);
		mat.setTexture("SpecularBuffer", gbuffer.specular);
		mat.setVector3("ClipInfo", m_ClipInfo);
		mat.setVector4("ProjInfo", m_ProjInfo);
	}

	void collectLights(Collection<Light> out) {
		out.clear();
		for(Spatial c : vp.getScenes()) {
			collectLights(out, c);
		}
		if (out.isEmpty()) {
			if (ambient0 == null) {
				ambient0 = new AmbientLight();
				ambient0.setColor(ColorRGBA.White);
			}
			out.add(ambient0);
		}
	}

	void collectLights(Collection<Light> out, Spatial s) {
		for(Light l : s.getLocalLightList()) {
			out.add(l);
		}
		if (s instanceof Node) {
			for(Spatial c : ((Node)s).getChildren()) {
				collectLights(out, c);
			}
		}
	}

	void configureLightPeer(LightPeer out, Light l) {
		out.geom = null;
		out.shadowMapGen = null;
		boolean useShadow = findIfUseShadow(l);
		if (l instanceof AmbientLight) {
			LigthPeerTmpl4Ambient peer = ligthPeerTmpl4Ambient;
			peer.configure((AmbientLight)l);
			out.geom = peer.geom;
			out.shadowMapGen = peer.shadowMapGen;
			out.isGlobal = peer.isGlobal;
		} else if (l instanceof DirectionalLight) {
			LigthPeerTmpl4Directional peer = useShadow ? ligthPeerTmpl4DirectionalAndShadow : ligthPeerTmpl4Directional;
			peer.configure((DirectionalLight)l);
			out.geom = peer.geom;
			out.shadowMapGen = peer.shadowMapGen;
			out.isGlobal = peer.isGlobal;
		} else if (l instanceof SpotLight) {
			LigthPeerTmpl4Spot peer = useShadow ? ligthPeerTmpl4SpotAndShadow : ligthPeerTmpl4Spot;
			peer.configure((SpotLight)l);
			out.geom = peer.geom;
			out.shadowMapGen = peer.shadowMapGen;
			out.isGlobal = peer.isGlobal;
		} else if (l instanceof PointLight) {
			LigthPeerTmpl4Point peer = ligthPeerTmpl4Point;
			peer.configure((PointLight)l);
			out.geom = peer.geom;
			out.shadowMapGen = peer.shadowMapGen;
			out.isGlobal = peer.isGlobal;
		}
	}

	boolean findIfUseShadow(Light l) {
		return true;
	}

int debug = 10;
	//TODO optimize
	public void render(RenderQueue rq) {
		FrameBuffer fbOrig = vp.getOutputFrameBuffer();
		vp.getCamera().setPlaneState(0);
		collectLights(lights0);

		Renderer r = rm.getRenderer();
		r.setFrameBuffer(lbuffer.fb);
		r.setBackgroundColor(new ColorRGBA(0,0,0,0));
		//
		r.clearBuffers(true, false, true);
		LightPeer lp = new LightPeer();
		for(Light l : lights0) {
			configureLightPeer(lp, l);
			if (lp.geom == null) continue;
			if (debug > 0) {
				//System.out.println("lp : " + lp.geom + " ... " + lp.geom.getWorldTransform() + ".. "+ ((SpotLight)l).getDirection() + " .. " + (lp.shadowMapGen != null));
				debug--;
			}
			Geometry g = lp.geom;
			g.updateGeometricState();
			g.updateModelBound();
			Material mat = g.getMaterial();
			if (lp.shadowMapGen != null) {
					//TODO update params of the material, avoid to update params linked to "#define"
					//shadowMapGen4Spot.displayFrustum();
					//shadowMapGen4Spot.displayDebug();
					//rm.setForcedRenderState(rsShadow);
					lp.shadowMapGen.renderShadowMaps(g, rq, mat);
					r.setFrameBuffer(lbuffer.fb);
					//shadowMapGen4Spot.displayShadowMaps();
			}
			if (!lp.isGlobal) {
				mat.setVector3("LightPos", g.getWorldTranslation());
				// using a fullview quad for this pass is possible but less performent (eg when lighting part of the screen)

				rm.setWorldMatrix(g.getWorldMatrix());

				r.clearBuffers(false, false, true);
				mat.selectTechnique("LBufMask", rm);
				rm.setForcedRenderState(rsLBufMask);
				mat.render(g, rm);

				mat.selectTechnique("LBuf", rm);
				mat.setBoolean("FullView", false);
				rm.setForcedRenderState(rsLBuf);
				mat.render(g, rm);
			} else {
				if (l instanceof AmbientLight){
					mat.selectTechnique("LBufAmbiant", rm);
					rm.setForcedRenderState(rsAmbiant);
				} else {
					mat.selectTechnique("LBuf", rm);
					rm.setForcedRenderState(rs0);
				}
				mat.setBoolean("FullView", true);
				rm.setWorldMatrix(g.getWorldMatrix());
				mat.render(g, rm);
			}
			mat.selectTechnique("Default", rm);
		}
		if (showLightGeom) {
			rm.setForcedMaterial(null);
			rm.setForcedRenderState(null);
			r.clearBuffers(false, false, true);
			//debugGeomMat.selectTechnique("redbackface", rm);
			for(Light l : lights0) {
				configureLightPeer(lp, l);
				if (lp.isGlobal) continue;
				if (lp.geom == null) continue;
				Geometry g = lp.geom;
				rm.setWorldMatrix(g.getWorldMatrix());
				Material mat = g.getMaterial();
				debugGeomMat.setColor("Color", (ColorRGBA)mat.getParam("Color").getValue());
				debugGeomMat.render(g, rm);
			}
		}
		rm.setForcedTechnique(null);
		rm.setForcedRenderState(null);
		//rm.getRenderer().setFrameBuffer(vp.getOutputFrameBuffer());
		vp.setOutputFrameBuffer(fbOrig);
		r.setFrameBuffer(fbOrig);
	}

	public void dispose(){

	}
}
interface ShadowMapGenerator {
	public void renderShadowMaps(Geometry g, RenderQueue rq, Material mat);
}

class LightPeer {
	public Geometry geom;
	public ShadowMapGenerator shadowMapGen;
	public boolean isGlobal;
}

abstract class LightPeerTmpl<T extends Light> {
	public static Material setupGeom(Geometry geom, Pass4LBuffer lbuffer) {
		Material mat = new MaterialCustom(lbuffer.assetManager, "MatDefs/deferred/lbuffer.j3md");
		mat.setColor("Color",  ColorRGBA.White);
		mat.setTransparent(true);
		lbuffer.initMaterial(mat);

		geom.setMaterial(mat);
		geom.setShadowMode(ShadowMode.Off);
		geom.setQueueBucket(Bucket.Translucent);

		return mat;
	}

	public abstract void configure(T light);
}

class RSpotLightShadowRenderer extends SpotLightShadowRenderer implements ShadowMapGenerator{
	public Texture shadowMap0;
	private String[] shadowMapStringCache;
	private String[] lightViewStringCache;

	public RSpotLightShadowRenderer(AssetManager assetManager, int shadowMapSize) {
		super(assetManager, shadowMapSize);
		setFlushQueues(false);
		shadowMapStringCache = new String[nbShadowMaps];
		lightViewStringCache = new String[nbShadowMaps];

		for (int i = 0; i < nbShadowMaps; i++) {
			shadowMapStringCache[i] = "ShadowMap" + i;
			lightViewStringCache[i] = "LightViewProjectionMatrix" + i;
		}

		//shadowMap0 = shadowMaps[0];
		//shadowMap0 = shadowFB[0].getColorBuffer().getTexture();
		shadowMap0 = shadowFB[0].getDepthBuffer().getTexture();
	}

	public void initMaterial(Material mat) {
		super.setPostShadowMaterial(mat);
		//mat.setFloat("ShadowMapSize", shadowMapSize);
		mat.setBoolean("HardwareShadows", shadowCompareMode == CompareMode.Hardware);
		mat.setInt("FilterMode", edgeFilteringMode.getMaterialParamValue());
		mat.setFloat("PCFEdge", edgesThickness);
		mat.setFloat("ShadowIntensity", shadowIntensity);
	}

	public void renderShadowMaps(Geometry g, RenderQueue rq, Material mat) {
		preFrame(0);
		postQueue(rq);

		for (int j = 0; j < nbShadowMaps; j++) {
			mat.setMatrix4(lightViewStringCache[j], lightViewProjectionsMatrices[j]);
		}
		for (int j = 0; j < nbShadowMaps; j++) {
			mat.setTexture(shadowMapStringCache[j], shadowMaps[j]);
		}
		//mat.setFloat("ShadowMapSize", shadowMapSize);
		//mat.setBoolean("HardwareShadows", shadowCompareMode == CompareMode.Hardware);
		//mat.setInt("FilterMode", edgeFilteringMode.getMaterialParamValue());
		//mat.setFloat("PCFEdge", edgesThickness);
		//mat.setFloat("ShadowIntensity", shadowIntensity);

		//setMaterialParameters(mat);

		//At least one material of the receiving geoms does not support the post shadow techniques
		//so we fall back to the forced material solution (transparent shadows won't be supported for these objects)
//		if (needsfallBackMaterial) {
//			setPostShadowParams();
//		}
	}

	public void displayShadowMaps() {
		displayShadowMap(renderManager.getRenderer());
	}
}

class RDirectionalLightShadowRenderer extends DirectionalLightShadowRenderer implements ShadowMapGenerator{
	public Texture shadowMap0;
	private String[] shadowMapStringCache;
	private String[] lightViewStringCache;

	public RDirectionalLightShadowRenderer(AssetManager assetManager, int shadowMapSize, int nbSplits) {
		super(assetManager, shadowMapSize, nbSplits);
		setFlushQueues(false);
		shadowMapStringCache = new String[nbShadowMaps];
		lightViewStringCache = new String[nbShadowMaps];

		for (int i = 0; i < nbShadowMaps; i++) {
			shadowMapStringCache[i] = "ShadowMap" + i;
			lightViewStringCache[i] = "LightViewProjectionMatrix" + i;
		}

		//shadowMap0 = shadowMaps[0];
		//shadowMap0 = shadowFB[0].getColorBuffer().getTexture();
		shadowMap0 = shadowFB[0].getDepthBuffer().getTexture();
	}

	public void initMaterial(Material mat) {
		mat.setFloat("ShadowMapSize", shadowMapSize);
		mat.setBoolean("HardwareShadows", shadowCompareMode == CompareMode.Hardware);
		mat.setInt("FilterMode", edgeFilteringMode.getMaterialParamValue());
		mat.setFloat("PCFEdge", edgesThickness);
		mat.setFloat("ShadowIntensity", shadowIntensity);
		super.setPostShadowMaterial(mat);
		super.setMaterialParameters(mat);
	}

	public void renderShadowMaps(Geometry g, RenderQueue rq, Material mat) {
		light.setColor((ColorRGBA)g.getMaterial().getParam("Color").getValue());
		light.setDirection((Vector3f)g.getMaterial().getParam("LightDir").getValue());
		preFrame(0);
		postQueue(rq);

		for (int j = 0; j < nbShadowMaps; j++) {
			mat.setMatrix4(lightViewStringCache[j], lightViewProjectionsMatrices[j]);
		}
		for (int j = 0; j < nbShadowMaps; j++) {
			mat.setTexture(shadowMapStringCache[j], shadowMaps[j]);
		}
	}

	public void displayShadowMaps() {
		displayShadowMap(renderManager.getRenderer());
	}
}
class LigthPeerTmpl4Ambient extends LightPeerTmpl<AmbientLight> {
	final Geometry geom;
	final ShadowMapGenerator shadowMapGen = null;
	final boolean isGlobal = true;

	LigthPeerTmpl4Ambient(Pass4LBuffer lbuffer) {
		geom = new Geometry(this.getClass().getName(), new Quad(1f, 1f));
		geom.setCullHint(Spatial.CullHint.Never);
		LightPeerTmpl.setupGeom(geom, lbuffer);
	}

	public void configure(AmbientLight light) {
		geom.setName("peer." + light.getName());
		Material mat = geom.getMaterial();
		mat.setColor("Color", light.getColor());
	}
}

class LigthPeerTmpl4Directional extends LightPeerTmpl<DirectionalLight> {
	final Geometry geom;
	final RDirectionalLightShadowRenderer shadowMapGen;
	final boolean isGlobal = true;

	LigthPeerTmpl4Directional(Pass4LBuffer lbuffer, boolean useShadow) {
		geom = new Geometry(this.getClass().getName(), new Quad(1f, 1f));
		geom.setCullHint(Spatial.CullHint.Never);
		Material mat = LightPeerTmpl.setupGeom(geom, lbuffer);

		if (useShadow) {
			shadowMapGen = new RDirectionalLightShadowRenderer(lbuffer.assetManager, 2048, 3);
			shadowMapGen.initialize(lbuffer.rm, lbuffer.vp);
			shadowMapGen.setShadowZExtend(1000);
			shadowMapGen.initMaterial(mat);
			mat.setFloat("PCFEdge", 1);
		} else {
			shadowMapGen = null;
		}
	}

	public void configure(DirectionalLight light) {
		geom.setName("peer." + light.getName());
		Material mat = geom.getMaterial();
		mat.setColor("Color", light.getColor());
		mat.setVector3("LightDir", light.getDirection());
		if (shadowMapGen != null) {
			shadowMapGen.setLight(light);
		}
	}
}

class LigthPeerTmpl4Spot extends LightPeerTmpl<SpotLight> {
	final Geometry geom;
	final RSpotLightShadowRenderer shadowMapGen;
	final boolean isGlobal = false;

	private final Quaternion quat0 = new Quaternion();
	private final Quaternion quat1 = new Quaternion();

	LigthPeerTmpl4Spot(Pass4LBuffer lbuffer, boolean useShadow) {
		geom = new Geometry(this.getClass().getName(), Helpers4Mesh.newCone(16, 1.0f, 0.5f));
		Material mat = LightPeerTmpl.setupGeom(geom, lbuffer);
		quat0.lookAt(new Vector3f(0,-1,0), Vector3f.UNIT_Y);
		quat0.normalizeLocal();

		if (useShadow) {
			shadowMapGen = new RSpotLightShadowRenderer(lbuffer.assetManager, 2048);
			shadowMapGen.initialize(lbuffer.rm, lbuffer.vp);
			//shadowMapGen.setShadowZExtend(1000);
			shadowMapGen.initMaterial(mat);
			mat.setFloat("PCFEdge", 1);
		} else {
			shadowMapGen = null;
		}
	}

	public void configure(SpotLight light) {
		geom.setName("peer." + light.getName());
		geom.setLocalTranslation(light.getPosition()); // TODO find the world position (not relative to attach)
		quat1.lookAt(light.getDirection(), Vector3f.UNIT_Y);
		quat1.normalizeLocal();
		quat1.multLocal(quat0);
		geom.setLocalRotation(quat1);
		Material mat = geom.getMaterial();
		mat.setColor("Color", light.getColor());
		mat.setFloat("LightFallOffDist", Math.abs(light.getSpotRange()));
		//mat.setVector3("LightDir", light.getDirection());
		float radius = FastMath.sin(light.getSpotOuterAngle()) * light.getSpotRange();
		geom.setLocalScale(radius, light.getSpotRange(), radius);
		if (shadowMapGen != null) {
			shadowMapGen.setLight(light);
		}
	}
}

class LigthPeerTmpl4Point extends LightPeerTmpl<PointLight> {
	final Geometry geom;
	final ShadowMapGenerator shadowMapGen = null;
	final boolean isGlobal = false;

	LigthPeerTmpl4Point(Pass4LBuffer lbuffer, boolean useShadow) {
		geom = new Geometry(this.getClass().getName(), new Sphere(16, 16, 0.5f));
		LightPeerTmpl.setupGeom(geom, lbuffer);
	}

	public void configure(PointLight light) {
		geom.setName("peer." + light.getName());
		geom.setLocalTranslation(light.getPosition()); // TODO find the world position (not relative to attach)
		Material mat = geom.getMaterial();
		mat.setColor("Color", light.getColor());
		mat.setFloat("LightFallOffDist", Math.abs(light.getRadius()*0.25f));
		geom.setLocalScale(light.getRadius());
	}
}
