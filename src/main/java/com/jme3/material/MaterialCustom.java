package com.jme3.material;

import com.jme3.asset.AssetManager;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;

public class MaterialCustom extends Material {
	public MaterialCustom(MaterialDef def) {
		super(def);
	}

	public MaterialCustom(AssetManager contentMan, String defName) {
		super(contentMan, defName);
	}

	/**
	 * Do not use this constructor. Serialization purposes only.
	 */
	public MaterialCustom() {
	}

	public void render(Geometry geom, RenderManager rm) {
		//super.autoSelectTechnique(rm);
		Technique technique= super.getActiveTechnique();
		if (technique == null) {
			selectTechnique("Default", rm);
			technique= super.getActiveTechnique();
		}
		if ("Default".equals(technique.getDef().getName())) {
			TechniqueDef techDef = technique.getDef();
			if (!techDef.isUsingShaders() || technique.getShader().getId() < 1) return;
		}
		super.render(geom, rm);
	}
}
