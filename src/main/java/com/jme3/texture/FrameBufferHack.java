package com.jme3.texture;

import java.lang.reflect.Field;

import com.jme3.texture.FrameBuffer.RenderBuffer;

public class FrameBufferHack {

	public static void setDepthStencilAttachment(FrameBuffer fb) {
		try {
			Field field = FrameBuffer.class.getDeclaredField("depthBuf");
			field.setAccessible(true);
			RenderBuffer depthBuf = (RenderBuffer) field.get(fb);
			depthBuf.slot = -101;
		} catch(Exception exc){
			throw new RuntimeException("wrap :" + exc, exc);
		}
	}

	public static void setStencilAttachment(FrameBuffer fb) {
		try {
			Field field = FrameBuffer.class.getDeclaredField("depthBuf");
			field.setAccessible(true);
			RenderBuffer depthBuf = (RenderBuffer) field.get(fb);
			depthBuf.slot = -102;
		} catch(Exception exc){
			throw new RuntimeException("wrap :" + exc, exc);
		}
	}
}
