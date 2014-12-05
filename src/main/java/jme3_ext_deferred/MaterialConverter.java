package jme3_ext_deferred;

import lombok.RequiredArgsConstructor;

import com.jme3.asset.AssetManager;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.material.MaterialCustom;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.SceneGraphVisitorAdapter;
import com.jme3.texture.Texture;

@RequiredArgsConstructor
public class MaterialConverter extends SceneGraphVisitorAdapter {
	final AssetManager assetManager;
	final MatIdManager matIdManager;
	public MaterialCustom defaultMaterial;

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
	public static void copyRenderState(Material dest, Material src, boolean all) {
		RenderState s = src.getAdditionalRenderState();
		RenderState o = dest.getAdditionalRenderState();
		if (s.isApplyWireFrame()) o.setWireframe(s.isWireframe());
		if (s.isApplyCullMode()) o.setFaceCullMode(s.getFaceCullMode());
		if (s.isApplyPolyOffset()) o.setPolyOffset(s.getPolyOffsetFactor(), s.getPolyOffsetUnits());
		if (all) {
			if (s.isApplyAlphaFallOff()) o.setAlphaFallOff(s.getAlphaFallOff());
			if (s.isApplyAlphaTest()) o.setAlphaTest(s.isAlphaTest());
			if (s.isApplyBlendMode()) o.setBlendMode(s.getBlendMode());
			if (s.isApplyColorWrite()) o.setColorWrite(s.isApplyColorWrite());
			if (s.isApplyDepthTest()) o.setDepthTest(s.isDepthTest());
			if (s.isApplyDepthWrite()) o.setDepthWrite(s.isDepthWrite());
			if (s.isApplyPointSprite()) o.setPointSprite(s.isPointSprite());
			if (s.isStencilTest()) o.setStencil(s.isStencilTest(), s.getFrontStencilStencilFailOperation(), s.getFrontStencilDepthFailOperation(), s.getFrontStencilDepthPassOperation(), s.getBackStencilStencilFailOperation(), s.getBackStencilDepthFailOperation(), s.getBackStencilDepthPassOperation(), s.getFrontStencilFunction(), s.getBackStencilFunction());
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
			copyRenderState(m, m0, false);
			g.setMaterial(m);
		} else if (defaultMaterial != null){
			g.setMaterial(defaultMaterial);
		} else {
			g.setMaterial(toMaterialCustom(m0));
		}
	}

	public MaterialCustom toMaterialCustom(Material m0) {
		if (m0 instanceof MaterialCustom) {
			return (MaterialCustom) m0;
		}
		MaterialCustom b = new MaterialCustom(m0.getMaterialDef());
		for (MatParam p : m0.getParams()) {
			b.setParam(p.getName(), p.getVarType(), p.getValue());
		}
		b.setTransparent(m0.isTransparent());
		b.setReceivesShadows(m0.isReceivesShadows());
		copyRenderState(b, m0, true);
		return b;
	}

}
