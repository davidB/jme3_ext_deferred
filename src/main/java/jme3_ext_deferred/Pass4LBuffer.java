package jme3_ext_deferred;

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
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.FrameBufferHack;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

class Pass4LBuffer {
	public final TBuffer lbuffer;
	final GBuffer gbuffer;
	final ViewPort vp;
	final RenderManager rm;
	final Node lightsRoot;
	final GeometryList renderedLightGeometries = new GeometryList(new NullComparator());
	final Vector4f m_ProjInfo;
	final Vector3f m_ClipInfo;
	final Texture2D m_MatBuffer;
	final Texture m_AmbientBuffer;

	final RenderState rsLBufMask = new RenderState();
	final RenderState rsLBuf = new RenderState();
	final Geometry finalQuad;

	public Pass4LBuffer(int width, int height, ViewPort vp, RenderManager rm, AssetManager assetManager, Node lightsRoot, GBuffer gbuffer, Texture2D matBuffer, Texture ambientBuffer) {
		this.gbuffer = gbuffer;
		this.lbuffer = new TBuffer(width, height, Format.RGBA8);
		lbuffer.fb.setDepthTexture(gbuffer.depth);
		FrameBufferHack.setDepthStencilAttachment(lbuffer.fb);

		this.vp = vp;
		this.rm = rm;
		this.lightsRoot = lightsRoot;
		this.m_MatBuffer = matBuffer;
		this.m_AmbientBuffer = ambientBuffer;
		Camera cam = vp.getCamera();
		this.m_ProjInfo = Helpers.projInfo(cam, width, height);
		this.m_ClipInfo = Helpers.clipInfo(cam);

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

		finalQuad = new Geometry("finalQuad", new Quad(1, 1));
		finalQuad.setCullHint(Spatial.CullHint.Never);

	}

	//TODO optimize
	public void render() {
		renderedLightGeometries.clear();
		vp.getCamera().setPlaneState(0);
		lightsRoot.breadthFirstTraversal(new SceneGraphVisitor() {
			@Override
			public void visit(Spatial spatial) {
				if (!(spatial instanceof Geometry)) return;
				Geometry lightGeom = (Geometry) spatial;
				if(lightGeom.checkCulling(vp.getCamera())){
					renderedLightGeometries.add(lightGeom);
					lightGeom.runControlRender(rm, vp);
					Material mat = lightGeom.getMaterial();
					mat.setTexture("MatBuffer", m_MatBuffer);
					mat.setTexture("DepthBuffer", gbuffer.depth);
					mat.setTexture("NormalBuffer", gbuffer.normal);
					mat.setVector3("ClipInfo", m_ClipInfo);
					mat.setVector4("ProjInfo", m_ProjInfo);
					mat.setVector3("Position", lightGeom.getWorldTranslation());
				}
			}
		});

		Renderer r = rm.getRenderer();
		r.setFrameBuffer(lbuffer.fb);
		r.setBackgroundColor(new ColorRGBA(0,0,0,0));
		//GL11.glClearStencil(0);
		r.clearBuffers(true, false, true);
		int nb = 2;//renderedLightGeometries.size();
		for (int i = 0; i < nb; i++) {
			Geometry g = renderedLightGeometries.get(i);
			rm.setWorldMatrix(g.getWorldMatrix());
			Material mat = g.getMaterial();
			mat.selectTechnique("LBufMask", rm);
			r.clearBuffers(false, false, true);
			rm.setForcedRenderState(rsLBufMask);
			mat.render(g, rm);

			mat.selectTechnique("LBuf", rm);
			//mat.setBoolean("FullView", true);
			rm.setForcedRenderState(rsLBuf);
			//rm.setWorldMatrix(finalQuad.getWorldMatrix());
			//mat.render(finalQuad, rm);
			mat.render(g, rm);
		}

		rm.getRenderer().setFrameBuffer(vp.getOutputFrameBuffer());
		rm.setForcedTechnique(null);
		rm.setForcedRenderState(null);
	}

	public void dispose(){

	}
}