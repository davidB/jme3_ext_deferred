package jme3_ext_deferred

import com.jme3.asset.AssetManager
import com.jme3.material.Material
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.Geometry
import com.jme3.texture.Texture
import com.jme3.texture.Texture2D

package class Pass4Shade {
	final package ViewPort vp
	final package RenderManager rm
	Material mat
	Geometry finalQuad

	new(Geometry finalQuad, ViewPort vp, RenderManager rm, AssetManager assetManager, GBuffer gbuffer,
		Texture2D matBuffer, Texture aoBuffer, Texture lBuffer) {
		this.finalQuad = finalQuad
		this.vp = vp
		this.rm = rm
		this.mat = new Material(assetManager, "MatDefs/deferred/shading.j3md")
		mat.setBoolean("FullView", true)
		mat.setTexture("MatBuffer", matBuffer)
		mat.setTexture("DepthBuffer", gbuffer.depth)
		mat.setTexture("NormalBuffer", gbuffer.normal)
		mat.setTexture("AlbedoBuffer", gbuffer.albedo)
		mat.setTexture("AOBuffer", aoBuffer)
		mat.setTexture("LBuffer", lBuffer)
		mat.getAdditionalRenderState().setDepthTest(true)
		mat.getAdditionalRenderState().setDepthWrite(true)
	}

	def void render() {
		rm.getRenderer().setViewPort(0, 0, vp.getCamera().getWidth(), vp.getCamera().getHeight()) // rm.getRenderer().clearBuffers(true, false, false);
		// rm.renderGeometry(finalQuad);
		mat.render(finalQuad, rm) // vp.getQueue().addToQueue(finalQuad, Bucket.Opaque);
		// vp.getQueue().renderQueue(Bucket.Opaque, rm, vp.getCamera(), true);
	}

	def void dispose() {
	}

}
