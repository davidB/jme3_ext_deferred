package jme3_ext_deferred

import java.nio.ByteBuffer
import com.jme3.math.ColorRGBA
import com.jme3.texture.Image
import com.jme3.texture.Image.Format
import com.jme3.texture.Texture.MagFilter
import com.jme3.texture.Texture.MinFilter
import com.jme3.texture.Texture2D
import com.jme3.util.BufferUtils
import com.jme3.texture.image.ColorSpace

class MatIdManager {
	package Image tableImage
	package Texture2D tableTex
	ByteBuffer tableData
	int nextId = 0
	int entriesSize
	public final int defId

	private new(int entriesNb, int entriesSize) {
		this.entriesSize = entriesSize
		tableData = BufferUtils::createByteBuffer(entriesNb * entriesSize * 4) // TODO 3.1 : use new Image(this.nativeFormat.get(), this.pWidth, this.pHeight, this.jmeData, com.jme3.texture.image.ColorSpace.sRGB);
		tableImage = new Image(Format::RGBA8, entriesSize, entriesNb, tableData, null, ColorSpace.Linear)
		tableTex = new Texture2D(tableImage)
		//tableTex.setWrap(WrapMode::Clamp)
		tableTex.setMinFilter(MinFilter::NearestNoMipMaps)
		tableTex.setMagFilter(MagFilter::Nearest)
		defId = findMatId(ColorRGBA::BlackNoAlpha, ColorRGBA::BlackNoAlpha)
	}

	new() {
		this(256, 2)
	}

	def int findMatId(ColorRGBA diffuse, ColorRGBA specular) {
		if(diffuse === null || specular === null) return defId // TODO search for already existing MatId
		var int id = nextId
		nextId++
		tableData.position(id * entriesSize * 4)
		tableData.put(diffuse.asBytesRGBA())
		tableData.put(specular.asBytesRGBA())
		return id
	}

	def int size() {
		return nextId
	}

}
