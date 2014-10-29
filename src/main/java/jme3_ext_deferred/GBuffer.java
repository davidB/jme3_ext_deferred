package jme3_ext_deferred;

import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.Texture2D;

class GBuffer {
	public final FrameBuffer fb;
	public final Texture2D diffuse, normal, specular, depth;

	public GBuffer(int w, int h) {
		diffuse  = new Texture2D(w, h, Format.RGBA8);
		normal   = new Texture2D(w, h, Format.RGBA8);
		normal.setMinFilter(MinFilter.NearestNoMipMaps);
		normal.setMagFilter(MagFilter.Nearest);
		specular = new Texture2D(w, h, Format.RGBA8);
		depth    = new Texture2D(w, h, Format.Depth);

		fb = new FrameBuffer(w, h, 1);
		fb.setMultiTarget(true);
		fb.setDepthTexture(depth);
		fb.addColorTexture(diffuse);
		fb.addColorTexture(normal);
		fb.addColorTexture(specular);
	}
}
