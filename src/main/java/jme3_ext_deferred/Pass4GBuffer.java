package jme3_ext_deferred;

import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.texture.FrameBuffer;

class Pass4GBuffer {
	public final GBuffer gbuffer;
	final ViewPort vp;
	final RenderManager rm;

	public Pass4GBuffer(int width, int height, ViewPort vp, RenderManager rm){
		this.gbuffer = new GBuffer(width, height);
		this.vp = vp;
		this.rm = rm;
	}

	public void render() {
		FrameBuffer fbOrig = vp.getOutputFrameBuffer();

		//vp.setOutputFrameBuffer(fb);
		rm.getRenderer().setFrameBuffer(gbuffer.fb);
		rm.getRenderer().setBackgroundColor(ColorRGBA.BlackNoAlpha);
		rm.getRenderer().clearBuffers(true, true, true);
		//vp.getQueue().isQueueEmpty(Bucket.Opaque);
		rm.setForcedTechnique("GBuf");
		rm.renderViewPortQueues(vp, true);
		//vp.getQueue().renderQueue(Bucket.Opaque, rm, vp.getCamera(), true);
		//vp.getQueue().renderQueue(Bucket.Transparent, rm, vp.getCamera(), true);

		vp.setOutputFrameBuffer(fbOrig);
		rm.getRenderer().setFrameBuffer(fbOrig);

		rm.setForcedTechnique(null);
	}

	public void dispose() {
		rm.getRenderer().deleteFrameBuffer(gbuffer.fb);
	}
}