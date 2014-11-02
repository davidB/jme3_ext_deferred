package com.jme3.texture;

import java.lang.reflect.Field;

import com.jme3.renderer.lwjgl.LwjglRendererCustom;
import com.jme3.texture.FrameBuffer.RenderBuffer;

import static org.lwjgl.opengl.EXTFramebufferObject.*;
import static org.lwjgl.opengl.ARBFramebufferObject.*;
import static org.lwjgl.opengl.GL11.*;

public class FrameBufferHack {

	public static void setDepthStencilAttachment(FrameBuffer fb, LwjglRendererCustom r) {
		try {
	        Field field = FrameBuffer.class.getDeclaredField("depthBuf");
	        field.setAccessible(true);
	        RenderBuffer depthBuf = (RenderBuffer) field.get(fb);
	        depthBuf.slot = -101;
//	        if (depthBuf != null) {
//	        	if (depthBuf.getTexture() != null) {
//	        glFramebufferTexture2DEXT(GL_FRAMEBUFFER_EXT,
//	        		GL_DEPTH_STENCIL_ATTACHMENT, //convertAttachmentSlot(rb.getSlot()),
//	        		GL_TEXTURE_2D,
//	                depthBuf.getTexture().getImage().getId(),
//	                0);
//	        	} else {
//	    	        glFramebufferRenderbufferEXT(GL_FRAMEBUFFER_EXT,
//	        		GL_DEPTH_STENCIL_ATTACHMENT,
//	        		GL_RENDERBUFFER_EXT,
//	        		depthBuf.getId());
//
//	        }
//	        }
	        //GL_STENCIL_ATTACHMENT_EXT
		} catch(Exception exc){
			throw new RuntimeException("wrap :" + exc, exc);
		}
	}
}
