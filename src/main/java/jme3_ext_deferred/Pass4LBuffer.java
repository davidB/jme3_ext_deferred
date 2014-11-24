package jme3_ext_deferred;

import rx_ext.Iterable4AddRemove;

import com.jme3.asset.AssetManager;
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
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
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
	final Texture m_AmbientBuffer;

	final RenderState rsLBufMask = new RenderState();
	final RenderState rsLBuf = new RenderState();
	final RenderState rs0 = new RenderState();
	final RenderState rsAmbiant = new RenderState();
	final Geometry finalQuad;

	public Pass4LBuffer(int width, int height, ViewPort vp, RenderManager rm, AssetManager assetManager, Iterable4AddRemove<Geometry> lights, GBuffer gbuffer, Texture2D matBuffer, Texture ambientBuffer) {
		this.gbuffer = gbuffer;
		//this.lbuffer = new TBuffer(width, height, Format.RGB16F);
		this.lbuffer = new TBuffer(width, height, Format.RGB8);
		lbuffer.fb.setDepthTexture(gbuffer.depth);
		FrameBufferHack.setDepthStencilAttachment(lbuffer.fb);

		this.vp = vp;
		this.rm = rm;
		this.m_MatBuffer = matBuffer;
		this.m_AmbientBuffer = ambientBuffer;
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
		for(Geometry g : this.lights){addLight(g);}
	}

	void addLight(Geometry g) {
		Material mat = g.getMaterial();
		mat.setTexture("MatBuffer", m_MatBuffer);
		mat.setTexture("DepthBuffer", gbuffer.depth);
		mat.setTexture("NormalBuffer", gbuffer.normal);
		mat.setTexture("AlbedoBuffer", gbuffer.albedo);
		mat.setTexture("SpecularBuffer", gbuffer.specular);
		mat.setTexture("AOBuffer", m_AmbientBuffer);
		mat.setVector3("ClipInfo", m_ClipInfo);
		mat.setVector4("ProjInfo", m_ProjInfo);
	}

	//TODO optimize
	public void render() {
		renderedLightGeometries.clear();
		vp.getCamera().setPlaneState(0);
		Geometry ambiant = null;
		for(Geometry g : lights.data) {
			Boolean v = g.getUserData(Helpers4Lights.UD_Ambiant);
			if (v != null && v) {
				ambiant = g;
			}else if(g.checkCulling(vp.getCamera())){
				renderedLightGeometries.add(g);
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
			boolean global = (boolean) g.getUserData(Helpers4Lights.UD_Global);
			if (!global) {
				mat.setVector3("LightPos", g.getWorldTranslation());
				rm.setWorldMatrix(g.getWorldMatrix());

				mat.selectTechnique("LBufMask", rm);
				r.clearBuffers(false, false, true);
				rm.setForcedRenderState(rsLBufMask);
				mat.render(g, rm);

				// using a fullview quad for this pass is possible but less performant (eg when lighting part of the screen)
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
		}
		if (ambiant != null) {
			Material mat = ambiant.getMaterial();
			mat.selectTechnique("LBuf", rm);
			mat.setBoolean("FullView", true);
			rm.setForcedRenderState(rsAmbiant);
			rm.setWorldMatrix(finalQuad.getWorldMatrix());
			mat.render(finalQuad, rm);
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
		rm.getRenderer().setFrameBuffer(vp.getOutputFrameBuffer());
		rm.setForcedTechnique(null);
		rm.setForcedRenderState(null);
	}

	public void dispose(){

	}
}