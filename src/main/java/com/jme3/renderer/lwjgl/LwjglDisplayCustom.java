package com.jme3.renderer.lwjgl;

import org.lwjgl.opengl.ContextAttribs;

import com.jme3.system.lwjgl.LwjglDisplay;

public class LwjglDisplayCustom extends LwjglDisplay {
	protected ContextAttribs createContextAttribs() {
		ContextAttribs attr = new ContextAttribs(3, 2);
		// see https://www.opengl.org/wiki/Core_And_Compatibility_in_Contexts
		attr = attr.withProfileCore(true).withForwardCompatible(false).withProfileCompatibility(true);
		if (settings.getBoolean("GraphicsDebug")) {
			attr = attr.withDebug(true);
		}
		return attr;
	}

	@Override
	protected void initContextFirstTime() {
		//super.initContextFirstTime();
		renderer = new LwjglRendererCustom();
		((LwjglRendererCustom)renderer).initialize();

		// Init input
		if (keyInput != null) {
			keyInput.initialize();
		}

		if (mouseInput != null) {
			mouseInput.initialize();
		}

		if (joyInput != null) {
			joyInput.initialize();
		}
	}

}
