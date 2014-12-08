package jme3_ext_deferred;

import rx_ext.Iterable4AddRemove;

import com.jme3.asset.AssetManager;
import com.jme3.light.DirectionalLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.material.RenderState.StencilOperation;
import com.jme3.material.RenderState.TestFunction;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.NullComparator;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
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
	final Iterable4AddRemove<Geometry> lights;
	final GeometryList renderedLightGeometries = new GeometryList(new NullComparator());
	final Vector4f m_ProjInfo;
	final Vector3f m_ClipInfo;
	final Texture2D m_MatBuffer;

	final RenderState rsLBufMask = new RenderState();
	final RenderState rsLBuf = new RenderState();
	final RenderState rs0 = new RenderState();
	final RenderState rsAmbiant = new RenderState();
	final RenderState rsShadow = new RenderState();
	final Geometry finalQuad;
	protected Geometry ambiant0;

	final AssetManager assetManager;

	final RSpotLightShadowRenderer shadowMapGen4Spot;
	final RDirectionalLightShadowRenderer shadowMapGen4Directional;

	public Pass4LBuffer(int width, int height, ViewPort vp, RenderManager rm, AssetManager assetManager, Iterable4AddRemove<Geometry> lights, GBuffer gbuffer, Texture2D matBuffer) {
		this.gbuffer = gbuffer;
		this.lbuffer = new TBuffer(width, height, Format.RGB16F);
		//this.lbuffer = new TBuffer(width, height, Format.RGB8);
		lbuffer.fb.setDepthTexture(gbuffer.depth);
		FrameBufferHack.setDepthStencilAttachment(lbuffer.fb);

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
		rsLBufMask.setBlendMode(BlendMode.Color);

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
		rsLBuf.setFaceCullMode(FaceCullMode.Front);
		rsLBuf.setBlendMode(BlendMode.Additive);

		rs0.setDepthTest(false);
		rs0.setDepthWrite(false);
		rs0.setFaceCullMode(FaceCullMode.Back);
		rs0.setBlendMode(BlendMode.Additive);

		rsAmbiant.setDepthTest(false);
		rsAmbiant.setDepthWrite(false);
		rsAmbiant.setFaceCullMode(FaceCullMode.Back);
		rsAmbiant.setBlendMode(BlendMode.Additive);

		finalQuad = new Geometry("finalQuad", new Quad(1, 1));
		finalQuad.setCullHint(Spatial.CullHint.Never);

		this.lights = lights;
		this.lights.ar.add.subscribe(this::addLight);

		//TODO lazy creation of shadowMapGens
		//TODO allow to configure shadowMapGens (use a Provider)
		shadowMapGen4Spot = new RSpotLightShadowRenderer(assetManager, 2048, vp, rm);
		shadowMapGen4Spot.setShadowZExtend(1000);

		shadowMapGen4Directional = new RDirectionalLightShadowRenderer(assetManager, 2048, 3, vp, rm);
		shadowMapGen4Directional.setShadowZExtend(1000);

		for(Geometry g : this.lights){addLight(g);}
	}

	void addLight(Geometry g) {
		Material mat = g.getMaterial();
		mat.setTexture("MatBuffer", m_MatBuffer);
		mat.setTexture("DepthBuffer", gbuffer.depth);
		mat.setTexture("NormalBuffer", gbuffer.normal);
		mat.setTexture("SpecularBuffer", gbuffer.specular);
		mat.setVector3("ClipInfo", m_ClipInfo);
		mat.setVector4("ProjInfo", m_ProjInfo);

		//init required default value, useless when no shadow, else updated by renderer
		mat.setFloat("ShadowMapSize", 2048);
		mat.setFloat("PCFEdge", 1);

		switch(Helpers4Lights.getShadowSourceMode(g)) {
		case Undef: break;
		case Spot:
			//TODO update params of the material, avoid to update params linked to "#define"
			shadowMapGen4Spot.initMaterial(mat);
			break;
		case Directional:
			//TODO update params of the material, avoid to update params linked to "#define"
			shadowMapGen4Directional.initMaterial(mat);
			break;
		}
	}

	//TODO optimize
	public void render(RenderQueue rq) {
		FrameBuffer fbOrig = vp.getOutputFrameBuffer();
		renderedLightGeometries.clear();
		vp.getCamera().setPlaneState(0);
		Geometry ambiant = null;
		if (lights.data.isEmpty()) {
			if (ambiant0 == null) {
				ambiant0 = Helpers4Lights.newAmbientLight("nolight", ColorRGBA.White, assetManager);
			}
			ambiant = ambiant0;
		} else {
			ambiant0 = null;
			for(Geometry g : lights.data) {
				if (g.getParent() == null) {
					lights.ar.remove.onNext(g);
					//TODO log a warning about use of invalid lights
					continue;
				}
				if (!Helpers4Lights.isEnabled(g)) continue;
				if (Helpers4Lights.isAmbiant(g)) {
					ambiant = g;
				}else if(Helpers4Lights.isGlobal(g) || g.checkCulling(vp.getCamera())){
					renderedLightGeometries.add(g);
				}
			}
		}

		Renderer r = rm.getRenderer();
		r.setFrameBuffer(lbuffer.fb);
		r.setBackgroundColor(new ColorRGBA(0,0,0,0));
		//
		r.clearBuffers(true, false, true);
		int nb = renderedLightGeometries.size();
		for (int i = 0; i < nb; i++) {
			Geometry g = renderedLightGeometries.get(i);
			Material mat = g.getMaterial();
			boolean global = Helpers4Lights.isGlobal(g);
			switch(Helpers4Lights.getShadowSourceMode(g)) {
				case Undef: break;
				case Spot : {
					//TODO update params of the material, avoid to update params linked to "#define"
					//shadowMapGen4Spot.displayFrustum();
					//shadowMapGen4Spot.displayDebug();
					//rm.setForcedRenderState(rsShadow);
					shadowMapGen4Spot.renderShadowMaps(g, rq, mat);
					r.setFrameBuffer(lbuffer.fb);
					//shadowMapGen4Spot.displayShadowMaps();
					break;
				}
				case Directional : {
					//TODO update params of the material, avoid to update params linked to "#define"
					//shadowMapGen4Spot.displayFrustum();
					//shadowMapGen4Spot.displayDebug();
					//rm.setForcedRenderState(rsShadow);
					shadowMapGen4Directional.renderShadowMaps(g, rq, mat);
					r.setFrameBuffer(lbuffer.fb);
					//shadowMapGen4Spot.displayShadowMaps();
					break;
				}
			}
			if (!global) {
				mat.setVector3("LightPos", g.getWorldTranslation());
				// using a fullview quad for this pass is possible but less performent (eg when lighting part of the screen)

				rm.setWorldMatrix(g.getWorldMatrix());

				mat.selectTechnique("LBufMask", rm);
				r.clearBuffers(false, false, true);
				rm.setForcedRenderState(rsLBufMask);
				mat.render(g, rm);

				mat.selectTechnique("LBuf", rm);
				mat.setBoolean("FullView", false);
				rm.setForcedRenderState(rsLBuf);
				mat.render(g, rm);
			} else {
				mat.selectTechnique("LBuf", rm);
				mat.setBoolean("FullView", true);
				rm.setForcedRenderState(rs0);
				rm.setWorldMatrix(finalQuad.getWorldMatrix());
				mat.render(finalQuad, rm);
			}
			mat.selectTechnique("Default", rm);
		}
		//TODO apply ambient in final shade/compisition ?
		if (ambiant != null) {
			Material mat = ambiant.getMaterial();
			mat.selectTechnique("LBufAmbiant", rm);
			mat.setBoolean("FullView", true);
			rm.setForcedRenderState(rsAmbiant);
			rm.setWorldMatrix(finalQuad.getWorldMatrix());
			mat.render(finalQuad, rm);
			mat.selectTechnique("Default", rm);
		}
		if (showLightGeom) {
			rm.setForcedMaterial(null);
			rm.setForcedRenderState(null);
			r.clearBuffers(false, false, true);
			//debugGeomMat.selectTechnique("redbackface", rm);
			for (int i = 0; i < nb; i++) {
				Geometry g = renderedLightGeometries.get(i);
				boolean global = (boolean) g.getUserData(Helpers4Lights.UD_Global);
				if (global) continue;
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

class RSpotLightShadowRenderer extends SpotLightShadowRenderer {
	Vector3f dir = new Vector3f(0,-1,0);
	public Texture shadowMap0;
	private String[] shadowMapStringCache;
	private String[] lightViewStringCache;

	public RSpotLightShadowRenderer(AssetManager assetManager, int shadowMapSize, ViewPort vp, RenderManager rm) {
		super(assetManager, shadowMapSize);
		initialize(rm, vp);
		setLight(new SpotLight());
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
		mat.setFloat("ShadowMapSize", shadowMapSize);
		mat.setBoolean("HardwareShadows", shadowCompareMode == CompareMode.Hardware);
		mat.setInt("FilterMode", edgeFilteringMode.getMaterialParamValue());
		mat.setFloat("PCFEdge", edgesThickness);
		mat.setFloat("ShadowIntensity", shadowIntensity);
	}

	public void renderShadowMaps(Geometry g, RenderQueue rq, Material mat) {
		light.setPosition(g.getWorldTranslation());
		//g.getWorldMatrix().mult(dir, light.getDirection());
		light.setDirection(dir);
		light.setSpotRange(100);
		//light.setSpotOuterAngle(spotOuterAngle);
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

//	@SuppressWarnings("fallthrough")
//	public void postQueue(RenderQueue rq) {
//		GeometryList occluders = rq.getShadowQueueContent(ShadowMode.Cast);
//		sceneReceivers = rq.getShadowQueueContent(ShadowMode.Receive);
//		skipPostPass = false;
//		if (sceneReceivers.size() == 0 || occluders.size() == 0) {
//			skipPostPass = true;
//			return;
//		}
//
//		updateShadowCams(viewPort.getCamera());
//
//		Renderer r = renderManager.getRenderer();
//		renderManager.setForcedMaterial(preshadowMat);
//		renderManager.setForcedTechnique("PreShadow");
//
//		for (int shadowMapIndex = 0; shadowMapIndex < nbShadowMaps; shadowMapIndex++) {
//
//			//            if (debugfrustums) {
//			//                doDisplayFrustumDebug(shadowMapIndex);
//			//            }
//			renderShadowMap(shadowMapIndex, occluders, sceneReceivers);
//
//		}
//
//		//debugfrustums = false;
//		if (flushQueues) {
//			occluders.clear();
//		}
//		//restore setting for future rendering
//		r.setFrameBuffer(viewPort.getOutputFrameBuffer());
//		renderManager.setForcedMaterial(null);
//		renderManager.setForcedTechnique(null);
//		renderManager.setCamera(viewPort.getCamera(), false);
//
//	}
//
//	protected void renderShadowMap(int shadowMapIndex, GeometryList occluders, GeometryList receivers) {
//		shadowMapOccluders = getOccludersToRender(shadowMapIndex, occluders, receivers, shadowMapOccluders);
//		Camera shadowCam = getShadowCam(shadowMapIndex);
//		//shadowCam.setFrustumNear(0.1f);
//		//saving light view projection matrix for this split
//		lightViewProjectionsMatrices[shadowMapIndex].set(shadowCam.getViewProjectionMatrix());
//		renderManager.setCamera(shadowCam, false);
//
//		renderManager.getRenderer().setFrameBuffer(shadowFB[shadowMapIndex]);
//		//renderManager.getRenderer().setBackgroundColor(ColorRGBA.BlackNoAlpha);
//		renderManager.getRenderer().setBackgroundColor(ColorRGBA.Red);
//		renderManager.getRenderer().clearBuffers(true, true, false);
//
//		// render shadow casters to shadow map
//		viewPort.getQueue().renderShadowQueue(shadowMapOccluders, renderManager, shadowCam, true);
//	}
}

class RDirectionalLightShadowRenderer extends DirectionalLightShadowRenderer {
	public Texture shadowMap0;
	private String[] shadowMapStringCache;
	private String[] lightViewStringCache;

	public RDirectionalLightShadowRenderer(AssetManager assetManager, int shadowMapSize, int nbSplits, ViewPort vp, RenderManager rm) {
		super(assetManager, shadowMapSize, nbSplits);
		initialize(rm, vp);
		setLight(new DirectionalLight());
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