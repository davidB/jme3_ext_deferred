package jme3_ext_deferred

import com.jme3.asset.AssetManager
import com.jme3.material.MatParam
import com.jme3.material.Material
import com.jme3.material.MaterialCustom
import com.jme3.material.RenderState
import com.jme3.math.ColorRGBA
import com.jme3.scene.Geometry
import com.jme3.scene.SceneGraphVisitorAdapter
import com.jme3.texture.Texture
import org.eclipse.xtend.lib.annotations.FinalFieldsConstructor

@FinalFieldsConstructor
class MaterialConverter extends SceneGraphVisitorAdapter {
	final package AssetManager assetManager
	final package MatIdManager matIdManager
	public MaterialCustom defaultMaterial

	@SuppressWarnings("unchecked") def static <T> T read(Material m, String name) {
		var MatParam p = m.getParam(name)
		return (if((p !== null)) p.getValue() else null ) as T
	}

	def static void copyColor(Material dest, String destName, Material src, String srcName) {
		var ColorRGBA v = read(src, srcName)
		if (v !== null) {
			dest.setColor(destName, v)
		}

	}

	def static void copyTexture(Material dest, String destName, Material src, String srcName) {
		var Texture v = read(src, srcName)
		if (v !== null) {
			dest.setTexture(destName, v)
		}

	}

	def static void copyBoolean(Material dest, String destName, Material src, String srcName) {
		var Boolean v = read(src, srcName)
		if (v !== null) {
			dest.setBoolean(destName, v)
		}

	}

	def static void copyRenderState(Material dest, Material src, boolean all) {
		var RenderState s = src.getAdditionalRenderState()
		var RenderState o = dest.getAdditionalRenderState()
		if(s.isApplyWireFrame()) o.setWireframe(s.isWireframe())
		if(s.isApplyCullMode()) o.setFaceCullMode(s.getFaceCullMode())
		if(s.isApplyPolyOffset()) o.setPolyOffset(s.getPolyOffsetFactor(), s.getPolyOffsetUnits())
		if (all) {
			if(s.isApplyAlphaFallOff()) o.setAlphaFallOff(s.getAlphaFallOff())
			if(s.isApplyAlphaTest()) o.setAlphaTest(s.isAlphaTest())
			if(s.isApplyBlendMode()) o.setBlendMode(s.getBlendMode())
			if(s.isApplyColorWrite()) o.setColorWrite(s.isApplyColorWrite())
			if(s.isApplyDepthTest()) o.setDepthTest(s.isDepthTest())
			if(s.isApplyDepthWrite()) o.setDepthWrite(s.isDepthWrite())
			if(s.isApplyPointSprite()) o.setPointSprite(s.isPointSprite())
			if(s.isStencilTest()) o.setStencil(s.isStencilTest(), s.getFrontStencilStencilFailOperation(),
				s.getFrontStencilDepthFailOperation(), s.getFrontStencilDepthPassOperation(),
				s.getBackStencilStencilFailOperation(), s.getBackStencilDepthFailOperation(),
				s.getBackStencilDepthPassOperation(), s.getFrontStencilFunction(), s.getBackStencilFunction())
		}

	}

	override void visit(Geometry g) {
		var Material m0 = g.getMaterial()
		if ("Common/MatDefs/Light/Lighting.j3md".equals(m0.getMaterialDef().getAssetName())) {
			var Material m = new MaterialCustom(assetManager, "MatDefs/deferred/gbuffer.j3md")
			// material.setBoolean("UseMaterialColors", true);
			// material.setColor("Ambient",  ambient.clone());
			// m.setInt("MatId", matIdManager.findMatId(read(m0, "Diffuse"), read(m0, "Specular")));
			m.setInt("MatId", matIdManager.defId)
			copyColor(m, "Color", m0, "Diffuse")
			copyTexture(m, "ColorMap", m0, "DiffuseMap")
			copyColor(m, "Specular", m0, "Specular")
			copyTexture(m, "SpecularMap", m0, "SpecularMap") // material.setFloat("Shininess", shininess); // prevents "premature culling" bug
			m.setFloat("AlphaDiscardThreshold", 0.5f)
			copyTexture(m, "NormalMap", m0, "NormalMap")
			copyTexture(m, "AlphaMap", m0, "AlphaMap")
			copyRenderState(m, m0, false)
			if (g.isGrouped()) {
				g.getParent().setMaterial(m)
			} else {
				g.setMaterial(m)
			}
		} else if (defaultMaterial !== null) {
			g.setMaterial(defaultMaterial)
		} else {
			g.setMaterial(toMaterialCustom(m0))
		}
	}

	def MaterialCustom toMaterialCustom(Material m0) {
		if (m0 instanceof MaterialCustom) {
			return m0
		}
		var MaterialCustom b = new MaterialCustom(m0.getMaterialDef())
		for (MatParam p : m0.getParams()) {
			b.setParam(p.getName(), p.getVarType(), p.getValue())
		}
		b.setTransparent(m0.isTransparent())
		b.setReceivesShadows(m0.isReceivesShadows())
		copyRenderState(b, m0, true)
		return b
	}

}
