package jme3_ext_deferred;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;

/**
 * Ambient Occlusion
 *
 * @see https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-core/src/main/java/com/jme3/shader/UniformBinding.java
 * @see http://floored.com/blog/2013/ssao-screen-space-ambient-occlusion.html
 * @see http://issamkhalil.wordpress.com/2013/07/22/implementing-ssao-part-1/
 * @see http://wili.cc/research/ffao/
 *
 * @author David Bernard
 */
public class Pass4AO {
	final GBuffer gbuffer;
	final Geometry finalQuad;
	final ViewPort vp;
	final RenderManager rm;

	final TBuffer aobuffer;
	public final Material aoMat;
	final TBuffer aoBlurHbuffer;
	public final Material aoBlurMat;
	final TBuffer aoBlurVbuffer;
	public final Texture finalTex;

	public Pass4AO(int width, int height, ViewPort vp, RenderManager rm, AssetManager assetManager, GBuffer gbuffer, Geometry finalQuad, boolean useNormalBuffer){
		this.gbuffer = gbuffer;
		this.finalQuad = finalQuad;
		this.vp = vp;
		this.rm = rm;
		this.aobuffer = new TBuffer(width, height, Format.RGB8);
		this.aobuffer.tex.setMinFilter(MinFilter.BilinearNearestMipMap);
		this.aobuffer.tex.setMagFilter(MagFilter.Bilinear);
		this.aoBlurHbuffer = new TBuffer(width, height, Format.RGB8);
		this.aoBlurVbuffer = new TBuffer(width, height, Format.RGB8);
		this.finalTex = aoBlurVbuffer.tex;
		this.aoMat = new Material(assetManager, "MatDefs/deferred/sao.j3md");
		this.aoBlurMat = new Material(assetManager, "MatDefs/deferred/saoblur.j3md");
		initMaterials(useNormalBuffer);
	}

	void initMaterials(boolean useNormalBuffer) {
		Camera cam = vp.getCamera();
		int w = aobuffer.fb.getWidth();
		int h = aobuffer.fb.getHeight();
		Vector4f m_ProjInfo = Helpers.projInfo(cam, w, h);
		double m_ProjScale = Helpers.projScale(cam, w, h);

		aoMat.getAdditionalRenderState().setDepthTest(false);
		aoMat.getAdditionalRenderState().setDepthWrite(false);
		aoMat.setBoolean("FullView", true);
		aoMat.setVector4("ProjInfo", m_ProjInfo);
		aoMat.setFloat("ProjScale", (float)m_ProjScale);
		aoMat.setVector3("SampleRadius", new Vector3f(1.5f, 1.5f * 1.5f, 1/(1.5f * 1.5f)));
		aoMat.setFloat("Intensity", 1.0f);
		aoMat.setFloat("Bias", 0.01f);
		/*
		//aoMat = new Material(assetManager, "sandbox/MatSsao.j3md");
        aoMat.setTexture("RandomMap", random);
        aoMat.setFloat("SampleRadius", 5.1f);
        aoMat.setFloat("Intensity", 1.5f);
        aoMat.setVector2("Scale", new Vector2f(0.2f, 0.2f));
        aoMat.setFloat("Bias", 0.1f);
        aoMat.setParam("Samples", VarType.Vector2Array, new Vector2f[]{new Vector2f(1.0f, 0.0f), new Vector2f(-1.0f, 0.0f), new Vector2f(0.0f, 1.0f), new Vector2f(0.0f, -1.0f)});;
		Texture random = assetManager.loadTexture("Common/MatDefs/SSAO/Textures/random.png");
        random.setWrap(Texture.WrapMode.Repeat);
        aoMat.setTexture("RandomMap", random);
        aoMat.setFloat("SampleRadius", 1.5f);
        aoMat.setParam("Samples", VarType.Vector2Array, new Vector2f[]{new Vector2f(1.0f, 0.0f), new Vector2f(-1.0f, 0.0f), new Vector2f(0.0f, 1.0f), new Vector2f(0.0f, -1.0f)});;
		mats.add(aoMat);
		 */

		aoBlurMat.getAdditionalRenderState().setDepthTest(false);
		aoBlurMat.getAdditionalRenderState().setDepthWrite(false);
		aoBlurMat.setBoolean("FullView", true);
		aoBlurMat.setVector4("ProjInfo", m_ProjInfo);

		if (useNormalBuffer) {
			aoMat.setTexture("NormalBuffer", gbuffer.normal);
			aoBlurMat.setTexture("NormalBuffer", gbuffer.normal);
		}
	}

	//TODO use 2 framebuffer/texture (eg by reusing the first one for the third render
	public void render() {
		FrameBuffer fbOrig = vp.getOutputFrameBuffer();

		aoMat.setTexture("DepthBuffer", gbuffer.depth);
		render(aoMat, aobuffer.fb);

		aoBlurMat.setTexture("Texture", aobuffer.tex);
		aoBlurMat.setVector2("Axis", new Vector2f(1,0));
		render(aoBlurMat, aoBlurHbuffer.fb);

		aoBlurMat.setTexture("Texture", aoBlurHbuffer.tex);
		aoBlurMat.setVector2("Axis", new Vector2f(0,1));
		render(aoBlurMat, aoBlurVbuffer.fb);

		vp.setOutputFrameBuffer(fbOrig);
		rm.getRenderer().setFrameBuffer(fbOrig);
	}

	void render(Material mat, FrameBuffer fb) {
		rm.getRenderer().setFrameBuffer(fb);
		rm.getRenderer().setBackgroundColor(ColorRGBA.BlackNoAlpha);
		rm.getRenderer().clearBuffers(true, false, false);
		mat.render(finalQuad, rm);
	}


	public void dispose() {
		rm.getRenderer().deleteFrameBuffer(gbuffer.fb);
	}
}