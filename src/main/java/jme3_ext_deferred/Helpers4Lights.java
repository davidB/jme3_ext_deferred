package jme3_ext_deferred;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;

public class Helpers4Lights {
	public static String UD_Global = "LightGlobal";
	public static String UD_Ambiant = "LightAmbiant";

	/**
	 *
	 * @param geo a closed concave volume (if not global, else a quad)
	 * @param color
	 * @param assetManager
	 * @return
	 */
	public static Geometry asPointLight(Geometry geo, ColorRGBA color, AssetManager assetManager) {
		Material mat = assetManager.loadMaterial("Materials/deferred/lighting.j3m");
		mat.setColor("Color", color);
		if (geo.getUserData(UD_Global) == null) {
			geo.setUserData(UD_Global, false);
		}
		geo.setMaterial(mat);
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
	public static Geometry asDirectionnalLight(Geometry geo, Vector3f direction, ColorRGBA color, AssetManager assetManager) {
		Material mat = assetManager.loadMaterial("Materials/deferred/lighting.j3m");
		mat.setColor("Color", color);
		mat.setVector3("LightDir", direction);
		if (geo.getUserData(UD_Global) == null) {
			geo.setUserData(UD_Global, true);
		}
		geo.setMaterial(mat);
		geo.updateGeometricState();
		geo.updateModelBound();
		return geo;
	}

	public static Geometry newPointLight(String name, float radius, ColorRGBA color, AssetManager assetManager) {
		Geometry geo = new Geometry(name, new Sphere(16, 16, radius));
		geo.setUserData(UD_Global, false); // should use volume+stencil or full screen quad
		return asPointLight(geo, color, assetManager);
	}

	public static Geometry newSpotLight(String name, float radiusX, float rangeY, ColorRGBA color, AssetManager assetManager) {
		Geometry geo = new Geometry(name, Helpers4Mesh.newCone(16, rangeY, radiusX));
		geo.setUserData(UD_Global, false); // should use volume+stencil or full screen quad
		return asPointLight(geo, color, assetManager);
	}

	public static Geometry newPointLightGlobal(String name, ColorRGBA color, AssetManager assetManager) {
		Geometry geo = new Geometry(name, new Quad(0.5f, 0.5f));
		geo.setUserData(UD_Global, true);
		return asPointLight(geo, color, assetManager);
	}

	public static Geometry newDirectionnalLight(String name, Vector3f direction, ColorRGBA color, AssetManager assetManager) {
		Geometry geo = new Geometry(name, new Quad(0.5f, 0.5f));
		geo.setUserData(UD_Global, true); // should use volume+stencil or full screen quad
		return asDirectionnalLight(geo, direction, color, assetManager);
	}

	/**
	 * @param color
	 * @param assetManager
	 * @return
	 */
	public static Geometry newAmbiantLight(String name, ColorRGBA color, AssetManager assetManager) {
		Material mat = assetManager.loadMaterial("Materials/deferred/lightingAmbiant.j3m");
		mat.setColor("Color", color);
		Geometry geo = new Geometry(name, new Quad(0.5f, 0.5f));
		geo.setUserData(UD_Ambiant, true);
		geo.setMaterial(mat);
		geo.updateGeometricState();
		geo.updateModelBound();
		return geo;
	}
}
