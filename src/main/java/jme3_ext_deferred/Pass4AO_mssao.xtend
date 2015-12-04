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
import com.jme3.shader.VarType

/**
 * Ambient Occlusion
 * @see https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-core/src/main/java/com/jme3/shader/UniformBinding.java
 * @see http://fr.slideshare.net/ozlael/mssao-presentation
 * @see [Multi-Resolution Screen-Space Ambient Occlusion](http://www.comp.nus.edu.sg/~lowkl/)
 * @author David Bernard
 */
//TODO optim use one array of FrameBuffer, and switch targetIndex
class Pass4AO_mssao {
    static val texId_ao = 0
    static val texId_blur = 1

	final GBuffer gbuffer
	final Geometry finalQuad
	final ViewPort vp
	final RenderManager rm
	public final Texture finalTex
	final GBufferMini[] gbuffers
	final TBuffer[] aobuffers
    final TBuffer[] blurbuffers
   	final TBuffer aobuffer0
	final Material aoMatFirst
    final Material aoMatMiddle
    final Material aoMatLast
    final Material blurMat
	final Material gbuffersMatDN
    final Material gbuffersMatG
    final float dMax = 2.5f // AO radius of influence
    
    val useBlur = false
    
	new(int width, int height, ViewPort vp, RenderManager rm, AssetManager assetManager, GBuffer gbuffer, int nbRes) {
		this.gbuffer = gbuffer
		this.vp = vp
		this.rm = rm
		this.gbuffersMatDN = new Material(assetManager, "MatDefs/deferred/gbufferdown.j3md")
        this.gbuffersMatG = new Material(assetManager, "MatDefs/deferred/gbufferdown.j3md")
		this.gbuffers = initGbuffers(width, height, nbRes)
		this.aobuffers = initAObuffers(width, height, nbRes)
		this.aobuffer0 = new TBuffer(width, height, Format.Luminance16F)
		this.blurbuffers = if (useBlur) initAObuffers(width, height, nbRes) else null
		//this.finalTex = gbuffers.get(gbuffers.length - 1).normal
		//this.finalTex = aobuffers.get(2).getTex(texId_ao)
        //this.finalTex = blurbuffers.get(2).getTex(0)
        this.finalTex = aobuffer0.tex
		this.aoMatFirst = new Material(assetManager, "MatDefs/deferred/mssao.j3md")
        this.aoMatMiddle = new Material(assetManager, "MatDefs/deferred/mssao.j3md")
        this.aoMatLast = new Material(assetManager, "MatDefs/deferred/mssao.j3md")
        this.blurMat = new Material(assetManager, "MatDefs/deferred/mssao_blur.j3md")
		this.finalQuad = new Geometry("finalQuad", new Quad(1, 1))
		finalQuad.setCullHint(Spatial::CullHint::Never) // finalQuad.setQueueBucket(Bucket.Opaque);
		finalQuad.setMaterial(null)
		initMaterials()
	}

	def GBufferMini[] initGbuffers(int width, int height, int nbRes) {
		val GBufferMini[] buffers = newArrayOfSize(nbRes - 1)
        val l = Math.max(width, height)
		for(var int i = 0; i < buffers.length; i++) {
			val ratioInv = 2 << i
			buffers.set(i, new GBufferMini(width / ratioInv, height / ratioInv))
            println("ratio: " + ratioInv + " ... " + (l / ratioInv));
		}
		buffers
	}

	def TBuffer[] initAObuffers(int width, int height, int nbRes) {
		val TBuffer[] buffers = newArrayOfSize(nbRes - 1)
		for(var int i = 0; i < buffers.length; i++) {
			val ratioInv = 2 << i
			buffers.set(i, new TBuffer(width / ratioInv, height / ratioInv, false, Format.RGB16F, Format.RGB16F))//, Format.Luminance16F))
		}
		buffers
	}

	def package void initMaterials() {
	    val float[] poissonDisk = #[
            -0.6116678f,  0.04548655f, -0.26605980f, -0.6445347f,
            -0.4798763f,  0.78557830f, -0.19723210f, -0.1348270f,
            -0.7351842f, -0.58396650f, -0.35353550f,  0.3798947f,
            0.1423388f,  0.39469180f, -0.01819171f,  0.8008046f,
            0.3313283f, -0.04656135f,  0.58593510f,  0.4467109f,
            0.8577477f,  0.11188750f,  0.03690137f, -0.9906120f,
            0.4768903f, -0.84335800f,  0.13749180f, -0.4746810f,
            0.7814927f, -0.48938420f,  0.38269190f,  0.8695006f
        ]
	    
        #[gbuffersMatDN, gbuffersMatG].forEach[m|
            m.getAdditionalRenderState().setDepthTest(false)
            m.getAdditionalRenderState().setDepthWrite(false)
            m.setBoolean("FullView", true)
        ]

		#[aoMatFirst, aoMatMiddle, aoMatLast].forEach[m|
    		m.getAdditionalRenderState().setDepthTest(false)
    		m.getAdditionalRenderState().setDepthWrite(false)
    		m.setBoolean("FullView", true)
    		m.setFloat("dMax", dMax) // AO radius of influence
    		m.setFloat("rMax", 7.0f) // maximum screen-space sampling radius
		]

        aoMatMiddle.setBoolean("Middle", true)
        
        aoMatLast.setBoolean("Last", true)
        aoMatLast.setParam("poissonDisk", VarType.FloatArray, poissonDisk)
        
        blurMat.setBoolean("FullView", true)
	}

	// TODO use 2 framebuffer/texture (eg by reusing the first one for the third render
	def void render() {
		var FrameBuffer fbOrig = vp.getOutputFrameBuffer()
		renderDownscaleGbuffers()
		renderFirstAO()
		renderMiddlesAO()
		renderLastAO()
		vp.setOutputFrameBuffer(fbOrig)
		rm.getRenderer().setFrameBuffer(fbOrig)
	}

    private val resh = new Vector2f(0, 0)
	def renderDownscaleGbuffers() {
		for(var int i = 0; i < gbuffers.length; i++) {
			val buf = gbuffers.get(i)
			val mat = if (i == 0) {
                resh.set(gbuffer.fb.width, gbuffer.fb.height)
                gbuffersMatDN.setVector2("ResHigh", resh)
                gbuffersMatDN.setTexture("DepthBuffer", gbuffer.depth)
                gbuffersMatDN.setTexture("NormalBuffer", gbuffer.normal)
                gbuffersMatDN
            } else {
                val high = gbuffers.get(i - 1)
                resh.set(high.fb.width, high.fb.height)
                gbuffersMatG.setVector2("ResHigh", resh)
                gbuffersMatG.setTexture("MiniGBuffer", high.normal)
                //println("i" +i + ".." + high.fb)
                gbuffersMatG
            } 
			render(mat, buf.fb)
		}
	}

    def blur(int i){
        if (!useBlur) return
        val m = blurMat
        val aobuffer = aobuffers.get(i)
        val buf = gbuffers.get(i)
        m.setTexture("MiniGBuffer", buf.normal)
        resh.set(buf.fb.width, buf.fb.height)
        m.setVector2("ResHigh", resh)
        m.setTexture("AOTex", aobuffer.getTex(texId_ao))
        //aobuffer.fb.targetIndex = texId_blur
        render(m, blurbuffers.get(i).fb)
    }
    
	def renderFirstAO() {
	    val m = aoMatFirst
		val i = aobuffers.length - 1
		val buf = gbuffers.get(i)
		val size = buf.fb.width
		//val r = size * dMax / (2.0f * Math.abs(Math.tan(fov * Math.PI / 180.0f / 2.0f)));
        val r = size * dMax / (2.0f * Math.abs(vp.camera.frustumTop / vp.camera.frustumNear))
		m.setFloat("r", r.floatValue)
		m.setTexture("MiniGBuffer", buf.normal)
		resh.set(buf.fb.width, buf.fb.height)
        m.setVector2("ResHigh", resh)
        aobuffers.get(i).fb.targetIndex = texId_ao
		render(m, aobuffers.get(i).fb)
		blur(i)
	}

	def renderMiddlesAO() {
        val m = aoMatMiddle
        for(var int i = aobuffers.length - 2; i > -1; i--) {
            val logbuf = gbuffers.get(i+1)
            m.setTexture("loResMiniGBuffer", logbuf.normal)
            m.setTexture("loResAOTex", aoTexInput(i+1))
            val gbuf = gbuffers.get(i)
            val size = gbuf.fb.width
            val r = size * dMax / (2.0f * Math.abs(vp.camera.frustumTop / vp.camera.frustumNear))
            m.setFloat("r", r.floatValue)
            m.setTexture("MiniGBuffer", gbuf.normal)
            resh.set(gbuf.fb.width, gbuf.fb.height)
            m.setVector2("ResHigh", resh)
            aobuffers.get(i).fb.targetIndex = texId_ao
            render(m, aobuffers.get(i).fb)
            blur(i)
        }
	}

    def aoTexInput(int i) {
        if (useBlur) {
            blurbuffers.get(i).getTex(0)
        } else {
            aobuffers.get(i).getTex(texId_ao)
        }
    }
	def renderLastAO() {
        val m = aoMatLast
        val i = 0
        val logbuf = gbuffers.get(i)
        m.setTexture("loResMiniGBuffer", logbuf.normal)
        m.setTexture("loResAOTex", aoTexInput(i))
        val gbuf = gbuffer
        val size = gbuf.fb.width
        val r = size * dMax / (2.0f * Math.abs(vp.camera.frustumTop / vp.camera.frustumNear))
        m.setFloat("r", r.floatValue)
        m.setFloat("dMax", dMax.floatValue)
        m.setTexture("DepthBuffer", gbuf.depth)
        m.setTexture("NormalBuffer", gbuf.normal)
        resh.set(gbuf.fb.width, gbuf.fb.height)
        m.setVector2("ResHigh", resh)
        render(m, aobuffer0.fb)
	}

	def package void render(Material mat, FrameBuffer fb) {
		rm.getRenderer().setFrameBuffer(fb)
		rm.getRenderer().setBackgroundColor(ColorRGBA::BlackNoAlpha)
		rm.getRenderer().clearBuffers(true, true, false)
		mat.render(finalQuad, rm)
	}

	def void dispose() {
		rm.getRenderer().deleteFrameBuffer(gbuffer.fb)
	}
}

class GBufferMini {
	public final FrameBuffer fb
	public final Texture2D normal
	//public final Texture2D depth

	// public final Texture2D custom0, custom1;
	new(int w, int h) {
		//depth = new Texture2D(w, h, 1, Format.RGBA8) // depth    = new texture(w, h, Format.Depth32);
        //depth = new Texture2D(w, h, Format.Depth24) // depth    = new texture(w, h, Format.Depth32);
        //depth.setMinFilter(MinFilter.NearestNoMipMaps)
        //depth.setMagFilter(MagFilter.Nearest)
		normal = new Texture2D(w, h, 1, Format.RGBA16F)
		normal.setMinFilter(MinFilter.NearestNoMipMaps)
		normal.setMagFilter(MagFilter.Nearest)
		fb = new FrameBuffer(w, h, 1)
		fb.setMultiTarget(false)
		//fb.setDepthTexture(depth)
        //fb.addColorTexture(depth)
		fb.addColorTexture(normal)
	}
}
