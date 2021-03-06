package jme3_ext_deferred

import com.jme3.math.ColorRGBA
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.texture.FrameBuffer

package class Pass4GBuffer {
	public final GBuffer gbuffer
	final package ViewPort vp
	final package RenderManager rm

	new(int width, int height, ViewPort vp, RenderManager rm) {
		this.gbuffer = new GBuffer(width, height)
		this.vp = vp
		this.rm = rm
	}

	def void render() {
		var FrameBuffer fbOrig = vp.getOutputFrameBuffer()
		// vp.setOutputFrameBuffer(fb);
		rm.getRenderer().setFrameBuffer(gbuffer.fb)
		rm.getRenderer().setBackgroundColor(ColorRGBA.BlackNoAlpha)
		rm.getRenderer().clearBuffers(true, true, true) // vp.getQueue().isQueueEmpty(Bucket.Opaque);
		rm.setForcedTechnique("GBuf")
		rm.renderViewPortQueues(vp, false) // vp.getQueue().renderQueue(Bucket.Opaque, rm, vp.getCamera(), false);
		// vp.getQueue().renderQueue(Bucket.Transparent, rm, vp.getCamera(), false);
		vp.setOutputFrameBuffer(fbOrig)
		rm.getRenderer().setFrameBuffer(fbOrig)
		rm.setForcedTechnique(null)
	}

	def void dispose() {
		rm.getRenderer().deleteFrameBuffer(gbuffer.fb)
	}

}
