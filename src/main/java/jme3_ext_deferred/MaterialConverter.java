package jme3_ext_deferred;

import lombok.RequiredArgsConstructor;

import com.jme3.asset.AssetManager;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.SceneGraphVisitorAdapter;

@RequiredArgsConstructor
public class MaterialConverter extends SceneGraphVisitorAdapter {
	final AssetManager assetManager;
	final MatIdManager matIdManager;
	public Material defaultMaterial;

	@SuppressWarnings("unchecked")
	public <T> T read(Material m, String name) {
		MatParam p = m.getParam(name);
		return (T) ((p != null)? p.getValue():null);
	}

	@Override
	public void visit(Geometry g) {
		Material m0 = g.getMaterial();
		if ("Common/MatDefs/Light/Lighting.j3md".equals(m0.getMaterialDef().getAssetName())) {
    		Material m = new Material(assetManager, "MatDefs/deferred/gbuffer.j3md");
            //material.setBoolean("UseMaterialColors", true);
            //material.setColor("Ambient",  ambient.clone());
    		m.setInt("MatId", matIdManager.findMatId(read(m, "Diffuse"), read(m, "Specular")));
            //material.setFloat("Shininess", shininess); // prevents "premature culling" bug
    		m.setFloat("AlphaDiscardThreshold", 0.5f);

            //if (diffuseMap != null)  material.setTexture("DiffuseMap", diffuseMap);
            //if (specularMap != null) material.setTexture("SpecularMap", specularMap);
            m.setTexture("NormalMap", read(m, "NormalMap"));
            m.setTexture("AlphaMap", read(m, "AlphaMap"));
            g.setMaterial(m);
		} else if (defaultMaterial != null){
			g.setMaterial(defaultMaterial);
		}
	}

}
