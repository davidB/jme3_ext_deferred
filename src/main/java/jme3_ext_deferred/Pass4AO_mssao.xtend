package jme3_ext_deferred

import com.jme3.asset.AssetManager
import com.jme3.material.Material
import com.jme3.math.ColorRGBA
import com.jme3.math.Vector2f
import com.jme3.math.Vector3f
import com.jme3.math.Vector4f
import com.jme3.renderer.Camera
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.Geometry
import com.jme3.texture.FrameBuffer
import com.jme3.texture.Image.Format
import com.jme3.texture.Texture
import com.jme3.texture.Texture.MagFilter
import com.jme3.texture.Texture.MinFilter
import com.jme3.texture.Texture2D
import com.jme3.scene.Spatial
import com.jme3.scene.shape.Quad

/**
 * Ambient Occlusion
 * @see https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-core/src/main/java/com/jme3/shader/UniformBinding.java
 * @see http://fr.slideshare.net/ozlael/mssao-presentation
 * @see [Multi-Resolution Screen-Space Ambient Occlusion](http://www.comp.nus.edu.sg/~lowkl/)
 * @author David Bernard
 */
class Pass4AO_mssao {
	final GBuffer gbuffer
	final Geometry finalQuad
	final ViewPort vp
	final RenderManager rm
	public final Texture finalTex
	final GBufferMini[] gbuffers
	final TBuffer[] aobuffers
	final TBuffer aobuffer
	final Material aoMatFirst
    final Material aoMatMiddle
    final Material aoMatLast
	final Material gbuffersMat
	final Float dMax = 2.5f

	new(int width, int height, ViewPort vp, RenderManager rm, AssetManager assetManager, GBuffer gbuffer, int nbRes) {
		this.gbuffer = gbuffer
		this.vp = vp
		this.rm = rm
		this.gbuffersMat = new Material(assetManager, "MatDefs/deferred/gbufferdown.j3md")
		this.gbuffersMat.getAdditionalRenderState().setDepthTest(false)
		this.gbuffersMat.getAdditionalRenderState().setDepthWrite(true)
		this.gbuffersMat.setBoolean("FullView", true)
		this.gbuffers = initGbuffers(width, height, nbRes)
		this.aobuffers = initAObuffers(width, height, nbRes)
		this.aobuffer = new TBuffer(width, height, Format.Luminance16F)
		this.finalTex = gbuffers.get(0).depth //aobuffer.tex
		//this.finalTex = aobuffers.get(aobuffers.length - 1).tex
		//this.finalTex = aobuffers.get(1).tex
		this.aoMatFirst = new Material(assetManager, "MatDefs/deferred/mssao.j3md")
        this.aoMatMiddle = new Material(assetManager, "MatDefs/deferred/mssao.j3md")
        this.aoMatLast = new Material(assetManager, "MatDefs/deferred/mssao.j3md")
		this.finalQuad = new Geometry("finalQuad", new Quad(1, 1))
		finalQuad.setCullHint(Spatial::CullHint::Never) // finalQuad.setQueueBucket(Bucket.Opaque);
		finalQuad.setMaterial(null)
		initMaterials()
	}

	def GBufferMini[] initGbuffers(int width, int height, int nbRes) {
		val GBufferMini[] buffers = newArrayOfSize(nbRes - 1)
		for(var int i = 0; i < buffers.length; i++) {
			val ratioInv = 2 << i
			buffers.set(i, new GBufferMini(width / ratioInv, height / ratioInv))
		}
		buffers
	}

	def TBuffer[] initAObuffers(int width, int height, int nbRes) {
		val TBuffer[] buffers = newArrayOfSize(nbRes - 1)
		for(var int i = 0; i < buffers.length; i++) {
			val ratioInv = 2 << i
			buffers.set(i, new TBuffer(width / ratioInv, height / ratioInv, Format.RGBA8))//, Format.Luminance16F))
		}
		buffers
	}

	def package void initMaterials() {
		val cam = vp.getCamera()
		val w = aobuffer.fb.getWidth()
		val h = aobuffer.fb.getHeight()
		val m_ProjInfo = Helpers::projInfo(cam, w, h)
		val m_ProjScale = Helpers::projScale(cam, w, h)

		#[aoMatFirst, aoMatMiddle, aoMatLast].forEach[m|
    		m.getAdditionalRenderState().setDepthTest(false)
    		m.getAdditionalRenderState().setDepthWrite(false)
    		m.setBoolean("FullView", true)
    		m.setVector4("ProjInfo", m_ProjInfo)
    		m.setFloat("ProjScale", m_ProjScale as float)
    		m.setFloat("dMax", dMax) // AO radius of influence
    		m.setFloat("rMax", 7.0f) // maximum screen-space sampling radius
		]

        aoMatMiddle.setBoolean("Middle", true);
        aoMatLast.setBoolean("Last", true);
	}

	// TODO use 2 framebuffer/texture (eg by reusing the first one for the third render
	def void render() {
		var FrameBuffer fbOrig = vp.getOutputFrameBuffer()
		renderDownscaleGbuffers()
		renderFirstAO()
		renderMiddlesAO()
		//renderLastAO()
		vp.setOutputFrameBuffer(fbOrig)
		rm.getRenderer().setFrameBuffer(fbOrig)
	}

	def renderDownscaleGbuffers() {
		gbuffersMat.setTexture("DepthBuffer", gbuffer.depth)
		gbuffersMat.setTexture("NormalBuffer", gbuffer.normal)
		for(var int i = 0; i < gbuffers.length; i++) {
			val buf = gbuffers.get(i)
			render(gbuffersMat, buf.fb)
			// setup for render
			if (i < gbuffers.length - 1) {
				gbuffersMat.setTexture("DepthBuffer", buf.depth)
				gbuffersMat.setTexture("NormalBuffer", buf.normal)
			}
		}
	}

	def renderFirstAO() {
	    val m = aoMatFirst
		val i = aobuffers.length - 1
		val buf = gbuffers.get(i)
		val size = buf.fb.width
		val fov = 65.238f //90f
	    //val fov = vp.camera...
		val r = size * dMax / (2.0f * Math.abs(Math.tan(fov * Math.PI / 180.0f / 2.0f)));
		m.setFloat("r", r.floatValue)
		m.setTexture("DepthBuffer", buf.depth)
		m.setTexture("NormalBuffer", buf.normal)
		render(m, aobuffers.get(i).fb)
	}

	def renderMiddlesAO() {
        val m = aoMatMiddle
        val fov = 90f
        for(var int i = aobuffers.length - 2; i > -1; i--) {
            val logbuf = gbuffers.get(i+1)
            m.setTexture("loResDepthTex", logbuf.depth)
            m.setTexture("loResNormTex", logbuf.normal)
            val loaobuf = aobuffers.get(i+1)
            m.setTexture("loResAOTex", loaobuf.tex)
            val gbuf = gbuffers.get(i)
            val size = gbuf.fb.width
            val r = size * dMax / (2.0f * Math.abs(Math.tan(fov * Math.PI / 180.0f / 2.0f)));
            m.setFloat("r", r.floatValue)
            m.setTexture("DepthBuffer", gbuf.depth)
            m.setTexture("NormalBuffer", gbuf.normal)
            render(m, aobuffers.get(i).fb)
        }
	}

	def renderLastAO() {
        val m = aoMatLast
        val fov = 90f
        val i = 0
        val logbuf = gbuffers.get(i)
        m.setTexture("loResDepthTex", logbuf.depth)
        m.setTexture("loResNormTex", logbuf.normal)
        val loaobuf = aobuffers.get(i)
        m.setTexture("loResAOTex", loaobuf.tex)
        val gbuf = gbuffer
        val size = gbuf.fb.width
        val r = size * dMax / (2.0f * Math.abs(Math.tan(fov * Math.PI / 180.0f / 2.0f)));
        m.setFloat("r", r.floatValue)
        m.setTexture("DepthBuffer", gbuf.depth)
        m.setTexture("NormalBuffer", gbuf.normal)
        render(m, aobuffer.fb)
	}

	def package void render(Material mat, FrameBuffer fb) {
		rm.getRenderer().setFrameBuffer(fb)
		rm.getRenderer().setBackgroundColor(ColorRGBA::BlackNoAlpha)
		rm.getRenderer().clearBuffers(true, false, false)
		mat.render(finalQuad, rm)
	}

	def void dispose() {
		rm.getRenderer().deleteFrameBuffer(gbuffer.fb)
	}
}

class GBufferMini {
	public final FrameBuffer fb
	public final Texture2D normal
	public final Texture2D depth

	// public final Texture2D custom0, custom1;
	new(int w, int h) {
		depth = new Texture2D(w, h, Format.Depth24) // depth    = new texture(w, h, Format.Depth32);
		normal = new Texture2D(w, h, Format.RGB8)
		normal.setMinFilter(MinFilter.NearestNoMipMaps)
		normal.setMagFilter(MagFilter.Nearest)
		fb = new FrameBuffer(w, h, 1)
		fb.setMultiTarget(false)
		fb.setDepthTexture(depth)
		fb.addColorTexture(normal)
	}
}
