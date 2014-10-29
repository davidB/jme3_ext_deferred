package jme3_ext_deferred;

import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture2D;

class TBuffer {
	public final FrameBuffer fb;
	public final Texture2D tex;

	public TBuffer(int w, int h, Format format) {
		tex  = new Texture2D(w, h, format);

		fb = new FrameBuffer(w, h, 1);
		fb.setMultiTarget(false);
		fb.addColorTexture(tex);
	}
}