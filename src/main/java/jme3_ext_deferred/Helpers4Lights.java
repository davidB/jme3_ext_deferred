package jme3_ext_deferred;

import com.jme3.asset.AssetManager;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.material.MaterialCustom;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;

public class Helpers4Lights {
	public static enum ShadowSourceMode {
		Undef
		, Spot
		, Directional
	};

	public static String UD_Enable = "LightEnable";
	public static String UD_Global = "LightGlobal";
	public static String UD_Ambient = "LightAmbient";
	public static String UD_ShadowSource = "ShadowSourceMode";

	protected static Geometry asLight(Geometry geo, boolean defaultGlobal) {
		if (geo.getUserData(UD_Global) == null) {
			geo.setUserData(UD_Global, defaultGlobal);
		}
		geo.setUserData(UD_Enable, true);
		// avoid light geometries to be processed like opaque/regular geometry (by other lights,...)
		geo.setShadowMode(ShadowMode.Off);
		geo.getMaterial().setTransparent(true);
		geo.setQueueBucket(Bucket.Translucent);
		geo.updateGeometricState();
		geo.updateModelBound();
		return geo;
	}

	/**
	 *
	 * @param geo a closed concave volume (if not global, else a quad)
	 * @param color
	 * @param assetManager
	 * @return
	 */
	public static Geometry asPointLight(Geometry geo, ColorRGBA color, AssetManager assetManager, Float lightFallOffDist) {
		//Material mat = assetManager.loadMaterial("Materials/deferred/lighting.j3m");
		Material mat = new MaterialCustom(assetManager, "MatDefs/deferred/lbuffer.j3md");
		mat.setColor("Color", color);
		if (lightFallOffDist != null){
			mat.setFloat("LightFallOffDist", Math.abs(lightFallOffDist));
		}
		geo.setMaterial(mat);
		return asLight(geo, false);
	}

	/**
	 *
	 * @param geo a closed concave volume (if not global, else a quad)
	 * @param color
	 * @param assetManager
	 * @return
	 */
	public static Geometry asDirectionalLight(Geometry geo, Vector3f direction, ColorRGBA color, AssetManager assetManager) {
		//Material mat = assetManager.loadMaterial("Materials/deferred/lighting.j3m");
		Material mat = new MaterialCustom(assetManager, "MatDefs/deferred/lbuffer.j3md");
		mat.setColor("Color", color);
		mat.setVector3("LightDir", direction);
		geo.setMaterial(mat);
		geo.setMaterial(mat);
		return asLight(geo, true);
	}

	public static Geometry newPointLight(String name, float radius, ColorRGBA color, AssetManager assetManager) {
		Geometry geo = new Geometry(name, new Sphere(16, 16, radius));
		geo.setUserData(UD_Global, false); // should use volume+stencil or full screen quad
		return asPointLight(geo, color, assetManager, radius);
	}

	public static Geometry newSpotLight(String name, float radiusX, float rangeY, ColorRGBA color, AssetManager assetManager) {
		Geometry geo = new Geometry(name, Helpers4Mesh.newCone(16, rangeY, radiusX));
		geo.setUserData(UD_Global, false); // should use volume+stencil or full screen quad
		return asPointLight(geo, color, assetManager, rangeY);
	}

	public static Geometry newPointLightGlobal(String name, ColorRGBA color, AssetManager assetManager) {
		Geometry geo = new Geometry(name, new Quad(0.5f, 0.5f));
		geo.setUserData(UD_Global, true);
		return asPointLight(geo, color, assetManager, null);
	}

	public static Geometry newDirectionalLight(String name, Vector3f direction, ColorRGBA color, AssetManager assetManager) {
		Geometry geo = new Geometry(name, new Quad(0.5f, 0.5f));
		return asDirectionalLight(geo, direction, color, assetManager);
	}

	/**
	 * @param color
	 * @param assetManager
	 * @return
	 */
	public static Geometry newAmbientLight(String name, ColorRGBA color, AssetManager assetManager) {
		//Material mat = assetManager.loadMaterial("Materials/deferred/lighting.j3m");
		Material mat = new MaterialCustom(assetManager, "MatDefs/deferred/lbuffer.j3md");
		mat.setColor("Color", color);
		Geometry geo = new Geometry(name, new Quad(0.5f, 0.5f));
		geo.setUserData(UD_Ambient, true);
		geo.setUserData(UD_Enable, true);
		geo.setMaterial(mat);
		geo.updateGeometricState();
		geo.updateModelBound();
		return geo;
	}

	public static boolean isLight(Geometry l) {
		return l.getUserData(UD_Enable) != null;
	}

	public static boolean isEnabled(Geometry l) {
		Object v = l.getUserData(UD_Enable);
		return (v != null)?(boolean)v : false;
	}

	public static Geometry setEnabled(Geometry l, boolean v) {
		l.setUserData(UD_Enable, v);
		return l;
	}

	public static boolean isGlobal(Geometry l) {
		Object v = l.getUserData(UD_Global);
		return (v != null)?(boolean)v : false;
	}

	public static boolean isAmbiant(Geometry l) {
		Object v = l.getUserData(UD_Ambient);
		return (v != null)?(boolean)v : false;
	}

	public static ShadowSourceMode getShadowSourceMode(Geometry l) {
		Object v = l.getUserData(UD_ShadowSource);
		return (v != null)? ShadowSourceMode.values()[(int)v] : ShadowSourceMode.Undef;
	}

	public static Geometry setShadowSourceMode(Geometry l, ShadowSourceMode v) {
		l.setUserData(UD_ShadowSource, v.ordinal());
		return l;
	}

	public static Geometry toGeometry(Light l, boolean castShadow, AssetManager assetManager) {
		if (l instanceof DirectionalLight) return toGeometry((DirectionalLight)l, castShadow, assetManager);
		if (l instanceof PointLight) return toGeometry((PointLight)l, castShadow, assetManager);
		if (l instanceof SpotLight) return toGeometry((SpotLight)l, castShadow, assetManager);
		if (l instanceof AmbientLight) return toGeometry((AmbientLight)l, assetManager);
		throw new IllegalArgumentException("unsupported Light type : " + l.getClass());
	}
	public static Geometry toGeometry(DirectionalLight l, boolean castShadow, AssetManager assetManager) {
		Geometry b = Helpers4Lights.newDirectionalLight(l.getName(), l.getDirection(), l.getColor(), assetManager);
		if (castShadow) Helpers4Lights.setShadowSourceMode(b, ShadowSourceMode.Directional);
		return b;
	}
	public static Geometry toGeometry(SpotLight l, boolean castShadow, AssetManager assetManager) {
		Geometry b = Helpers4Lights.newSpotLight(l.getName(), FastMath.sin(l.getSpotOuterAngle())* l.getSpotRange(), l.getSpotRange(), l.getColor(), assetManager);
		if (castShadow) Helpers4Lights.setShadowSourceMode(b, ShadowSourceMode.Spot);
		return b;
	}
	public static Geometry toGeometry(PointLight l, boolean castShadow, AssetManager assetManager) {
		Geometry b = Helpers4Lights.newPointLight(l.getName(), l.getRadius(), l.getColor(), assetManager);
		//if (castShadow) Helpers4Lights.setShadowSourceMode(b, ShadowSourceMode.Point);
		return b;
	}
	public static Geometry toGeometry(AmbientLight l, AssetManager assetManager) {
		Geometry b = Helpers4Lights.newAmbientLight(l.getName(), l.getColor(), assetManager);
		return b;
	}
}
