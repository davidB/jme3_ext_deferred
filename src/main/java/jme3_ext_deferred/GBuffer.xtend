package jme3_ext_deferred

import com.jme3.texture.FrameBuffer
import com.jme3.texture.Image.Format
import com.jme3.texture.Texture.MagFilter
import com.jme3.texture.Texture.MinFilter
import com.jme3.texture.Texture2D

package class GBuffer {
	public final FrameBuffer fb
	public final Texture2D normal
	public final Texture2D depth
	public final Texture2D albedo
	public final Texture2D specular

	// public final Texture2D custom0, custom1;
	new(int w, int h) {
		depth = new Texture2D(w, h, Format.Depth24Stencil8) // depth    = new texture(w, h, Format.Depth32);
		normal = new Texture2D(w, h, Format.RGBA8)
		normal.setMinFilter(MinFilter.NearestNoMipMaps)
		normal.setMagFilter(MagFilter.Nearest)
		albedo = new Texture2D(w, h, Format.RGBA8)
		specular = new Texture2D(w, h, Format.RGBA8) // custom0 = new texture(w, h, Format.RGBA8);
		// custom1  = new texture(w, h, Format.RGBA8);
		fb = new FrameBuffer(w, h, 1)
		fb.setMultiTarget(true)
		fb.setDepthTexture(depth)
		fb.addColorTexture(normal)
		fb.addColorTexture(albedo)
		fb.addColorTexture(specular) // fb.addColorTexture(custom0);
		// fb.addColorTexture(custom1);
	}

}
