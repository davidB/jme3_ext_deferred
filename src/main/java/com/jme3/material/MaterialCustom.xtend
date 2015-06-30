package com.jme3.material

import com.jme3.asset.AssetManager
import com.jme3.renderer.RenderManager
import com.jme3.scene.Geometry

class MaterialCustom extends Material {
	new(MaterialDef definition) {
		super(definition)
	}

	new(AssetManager contentMan, String defName) {
		super(contentMan, defName)
	}

	/** 
	 * Do not use this constructor. Serialization purposes only.
	 */
	new() {
	}

	override void render(Geometry geom, RenderManager rm) {
		// super.autoSelectTechnique(rm);
		var technique = super.getActiveTechnique()
		if (technique == null) {
			selectTechnique("Default", rm)
			technique = super.getActiveTechnique()
		}
		if(technique.getShader().getSources().size() == 0) return;

		super.render(geom, rm)
	}

}
