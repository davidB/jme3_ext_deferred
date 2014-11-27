package jme3_ext_deferred;

import lombok.RequiredArgsConstructor;

import com.jme3.asset.AssetManager;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.material.MaterialCustom;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.SceneGraphVisitorAdapter;
import com.jme3.texture.Texture;

@RequiredArgsConstructor
public class MaterialConverter extends SceneGraphVisitorAdapter {
	final AssetManager assetManager;
	final MatIdManager matIdManager;
	public Material defaultMaterial;

	@SuppressWarnings("unchecked")
	public static <T> T read(Material m, String name) {
		MatParam p = m.getParam(name);
		return (T) ((p != null)? p.getValue():null);
	}
	public static void copyColor(Material dest, String destName, Material src, String srcName) {
		ColorRGBA v = read(src, srcName);
		if (v != null) {
			dest.setColor(destName, v);
		}
	}
	public static void copyTexture(Material dest, String destName, Material src, String srcName) {
		Texture v = read(src, srcName);
		if (v != null) {
			dest.setTexture(destName, v);
		}
	}
	public static void copyBoolean(Material dest, String destName, Material src, String srcName) {
		Boolean v = read(src, srcName);
		if (v != null) {
			dest.setBoolean(destName, v);
		}
	}

	@Override
	public void visit(Geometry g) {
		Material m0 = g.getMaterial();
		if ("Common/MatDefs/Light/Lighting.j3md".equals(m0.getMaterialDef().getAssetName())) {
			Material m = new MaterialCustom(assetManager, "MatDefs/deferred/gbuffer.j3md");
			//material.setBoolean("UseMaterialColors", true);
			//material.setColor("Ambient",  ambient.clone());
			//m.setInt("MatId", matIdManager.findMatId(read(m0, "Diffuse"), read(m0, "Specular")));
			m.setInt("MatId", matIdManager.defId);
			copyColor(m, "Color", m0, "Diffuse");
			copyTexture(m, "ColorMap", m0, "DiffuseMap");
			copyColor(m, "Specular", m0, "Specular");
			copyTexture(m, "SpecularMap", m0, "SpecularMap");
			//material.setFloat("Shininess", shininess); // prevents "premature culling" bug
			m.setFloat("AlphaDiscardThreshold", 0.5f);

			copyTexture(m, "NormalMap", m0, "NormalMap");
			copyTexture(m, "AlphaMap", m0, "AlphaMap");
			g.setMaterial(m);
		} else if (defaultMaterial != null){
			g.setMaterial(defaultMaterial);
		}
	}

}
