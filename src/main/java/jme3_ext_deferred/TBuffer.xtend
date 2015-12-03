package jme3_ext_deferred

import com.jme3.texture.FrameBuffer
import com.jme3.texture.Image.Format
import com.jme3.texture.Texture2D

package class TBuffer {
	public final FrameBuffer fb
	
    new(int w, int h, Format... format) {
        this(w, h, format.length > 1, format)
    }
	
	new(int w, int h, boolean mrt, Format... format) {
		fb = new FrameBuffer(w, h, 1)
		fb.setMultiTarget(mrt)
		for (var int i = 0; i < format.length; i++) {
			var Texture2D t = new Texture2D(w, h, format.get(i))
			fb.addColorTexture(t)
		}
	}

    def getTex() { getTex(0) }
    def getTex(int i) { fb.getColorBuffer(i).texture }
}
