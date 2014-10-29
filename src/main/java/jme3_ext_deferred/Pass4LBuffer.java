package jme3_ext_deferred;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
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

	public Pass4LBuffer(int width, int height, ViewPort vp, RenderManager rm, AssetManager assetManager, Node lightsRoot, GBuffer gbuffer, Texture2D matBuffer, Texture ambientBuffer) {
		this.gbuffer = gbuffer;
		this.lbuffer = new TBuffer(width, height, Format.RGBA8);
		lbuffer.fb.setDepthBuffer(Format.Depth24Stencil8);
		lbuffer.fb.setDepthTexture(gbuffer.depth);
		this.vp = vp;
		this.rm = rm;
		this.lightsRoot = lightsRoot;
		this.m_MatBuffer = matBuffer;
		this.m_AmbientBuffer = ambientBuffer;
		Camera cam = vp.getCamera();
		this.m_ProjInfo = Helpers.projInfo(cam, width, height);
		this.m_ClipInfo = Helpers.clipInfo(cam);
	}

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
				    //material = new Material(assetManager, "DMonkey/SpotLight.j3md");
				    mat.setTexture("MatBuffer", m_MatBuffer);
				    mat.setTexture("DepthBuffer", gbuffer.depth);
				    mat.setTexture("NormalBuffer", gbuffer.normal);
				    mat.setVector3("ClipInfo", m_ClipInfo);
				    mat.setVector4("ProjInfo", m_ProjInfo);
				    mat.setVector3("Position", lightGeom.getWorldTranslation());
//				    mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Additive);
//				    //mat.getAdditionalRenderState().setStencil(enabled, _frontStencilStencilFailOperation, _frontStencilDepthFailOperation, _frontStencilDepthPassOperation, _backStencilStencilFailOperation, _backStencilDepthFailOperation, _backStencilDepthPassOperation, _frontStencilFunction, _backStencilFunction);
//				    mat.getAdditionalRenderState().setDepthTest(false);
//				    mat.getAdditionalRenderState().setDepthWrite(false);
				//    mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Front);
				}
			}
		});
		Renderer r = rm.getRenderer();
		r.setFrameBuffer(lbuffer.fb);
		r.setBackgroundColor(ColorRGBA.BlackNoAlpha);
		r.clearBuffers(true, false, false);
		rm.setForcedTechnique("LBuf");
		rm.renderGeometryList(renderedLightGeometries);

		rm.getRenderer().setFrameBuffer(vp.getOutputFrameBuffer());

		rm.setForcedTechnique(null);
	}

	public void dispose(){

	}
}