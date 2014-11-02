package com.jme3.renderer.lwjgl;

import com.jme3.system.lwjgl.LwjglDisplay;

public class LwjglDisplayCustom extends LwjglDisplay {
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
