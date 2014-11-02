package jme3_ext_deferred;

import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.Texture2D;

class GBuffer {
	public final FrameBuffer fb;
	public final Texture2D normal, depth;
	public final Texture2D custom0, custom1;

	public GBuffer(int w, int h) {
		depth    = new Texture2D(w, h, Format.Depth24Stencil8);
		normal   = new Texture2D(w, h, Format.RGBA8);
		normal.setMinFilter(MinFilter.NearestNoMipMaps);
		normal.setMagFilter(MagFilter.Nearest);
		custom0 = new Texture2D(w, h, Format.RGBA8);
		custom1  = new Texture2D(w, h, Format.RGBA8);

		fb = new FrameBuffer(w, h, 1);
		fb.setMultiTarget(true);
		fb.setDepthTexture(depth);
		fb.addColorTexture(normal);
		fb.addColorTexture(custom0);
		fb.addColorTexture(custom1);
	}
}
