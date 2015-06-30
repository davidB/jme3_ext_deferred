package com.jme3.texture

import java.lang.reflect.Field
import com.jme3.texture.FrameBuffer.RenderBuffer

class FrameBufferHack {
	def static void setDepthStencilAttachment(FrameBuffer fb) {
		try {
			var Field field = typeof(FrameBuffer).getDeclaredField("depthBuf")
			field.setAccessible(true)
			var RenderBuffer depthBuf = field.get(fb) as RenderBuffer
			depthBuf.slot = -101
		} catch (Exception exc) {
			throw new RuntimeException('''wrap :«exc»'''.toString, exc)
		}

	}

	def static void setStencilAttachment(FrameBuffer fb) {
		try {
			var Field field = typeof(FrameBuffer).getDeclaredField("depthBuf")
			field.setAccessible(true)
			var RenderBuffer depthBuf = field.get(fb) as RenderBuffer
			depthBuf.slot = -102
		} catch (Exception exc) {
			throw new RuntimeException('''wrap :«exc»'''.toString, exc)
		}

	}

}
