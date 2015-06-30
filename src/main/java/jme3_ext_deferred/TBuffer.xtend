package jme3_ext_deferred

import com.jme3.texture.FrameBuffer
import com.jme3.texture.Image.Format
import com.jme3.texture.Texture2D

package class TBuffer {
	public final FrameBuffer fb
	public final Texture2D tex

	new(int w, int h, Format... format) {
		fb = new FrameBuffer(w, h, 1)
		fb.setMultiTarget(false)
		tex = new Texture2D(w, h, format.get(0))
		fb.addColorTexture(tex)
		for (var int i = 1; i < format.length; i++) {
			var Texture2D t = new Texture2D(w, h, {
				val _rdIndx_format = i
				format.get(_rdIndx_format)
			})
			fb.addColorTexture(t)
		}

	}

}
