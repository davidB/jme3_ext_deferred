package jme3_ext_deferred;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.NullComparator;
import com.jme3.scene.Geometry;
import com.jme3.texture.Image.Format;

class Pass4LBuffer {
	public final TBuffer lbuffer;
	final GBuffer gbuffer;
	final ViewPort vp;
	final RenderManager rm;
	final GeometryList lightGeometries;
	final GeometryList renderedLightGeometries = new GeometryList(new NullComparator());

	public Pass4LBuffer(int width, int height, ViewPort vp, RenderManager rm, AssetManager assetManager, GeometryList lights,GBuffer gbuffer) {
		this.gbuffer = gbuffer;
		this.lbuffer = new TBuffer(width, height, Format.RGBA8);
		lbuffer.fb.setDepthBuffer(Format.Depth24Stencil8);
		lbuffer.fb.setDepthTexture(gbuffer.depth);
		this.vp = vp;
		this.rm = rm;
		this.lightGeometries = lights;
	}
	public void update(float tpf) {
		for(int i = 0; i < lightGeometries.size(); i++){
			vp.getCamera().setPlaneState(0);
			Geometry geom = lightGeometries.get(i);
			geom.updateLogicalState(tpf);
			geom.updateGeometricState();
		}
	}
	public void render() {
		renderedLightGeometries.clear();
		for(int i = 0; i < lightGeometries.size(); i++){
			vp.getCamera().setPlaneState(0);
			Geometry lightGeom = lightGeometries.get(i);
			if(lightGeom.checkCulling(vp.getCamera())){
				renderedLightGeometries.add(lightGeom);
				lightGeom.runControlRender(rm, vp);
				Material mat = lightGeom.getMaterial();
			    //material = new Material(assetManager, "DMonkey/SpotLight.j3md");
			    mat.setTexture("DiffuseBuffer", gbuffer.diffuse);
			    mat.setTexture("DepthBuffer", gbuffer.depth);
			    mat.setTexture("NormalBuffer", gbuffer.normal);
			    mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Additive);
			    //mat.getAdditionalRenderState().setStencil(enabled, _frontStencilStencilFailOperation, _frontStencilDepthFailOperation, _frontStencilDepthPassOperation, _backStencilStencilFailOperation, _backStencilDepthFailOperation, _backStencilDepthPassOperation, _frontStencilFunction, _backStencilFunction);
			    mat.getAdditionalRenderState().setDepthTest(false);
			    mat.getAdditionalRenderState().setDepthWrite(false);
			    mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Front);
			}
		}
		rm.getRenderer().setFrameBuffer(lbuffer.fb);
		rm.getRenderer().setBackgroundColor(ColorRGBA.BlackNoAlpha);
		rm.getRenderer().clearBuffers(true, false, true);
		rm.setForcedTechnique("LBuf");
		rm.renderGeometryList(renderedLightGeometries);

		rm.getRenderer().setFrameBuffer(vp.getOutputFrameBuffer());

		rm.setForcedTechnique(null);
	}

	public void dispose(){

	}
}