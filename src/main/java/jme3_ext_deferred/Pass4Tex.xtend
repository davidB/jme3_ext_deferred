package jme3_ext_deferred

import com.jme3.asset.AssetManager
import com.jme3.material.Material
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.Geometry
import com.jme3.texture.Texture

class Pass4Tex {
	final package Geometry finalQuad
	final package ViewPort vp
	final package RenderManager rm
	final package Material texMat

	new(Geometry finalQuad, ViewPort vp, RenderManager rm, AssetManager assetManager, Texture texture) {
		this.finalQuad = finalQuad
		this.vp = vp
		this.rm = rm
		this.texMat = new Material(assetManager, "MatDefs/debug/tex.j3md")
		texMat.setBoolean("FullView", true)
		texMat.setTexture("Texture", texture)
		//texMat.setBoolean("ROnly", true)
	}

	def void render() {
		rm.getRenderer().setViewPort(0, 0, vp.getCamera().getWidth(), vp.getCamera().getHeight())
		rm.getRenderer().clearBuffers(true, false, false) // rm.renderGeometry(finalQuad);
		texMat.render(finalQuad, rm) // vp.getQueue().addToQueue(finalQuad, Bucket.Opaque);
		// vp.getQueue().renderQueue(Bucket.Opaque, rm, vp.getCamera(), true);
	}

	def void dispose() {
	}

}
