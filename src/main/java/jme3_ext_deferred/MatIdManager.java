package jme3_ext_deferred;

import java.nio.ByteBuffer;

import lombok.Getter;

import com.jme3.math.ColorRGBA;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;

public class MatIdManager {
	@Getter Image tableImage;
	@Getter Texture2D tableTex;

	private ByteBuffer tableData;
	private int nextId = 0;
	private int entriesSize;
	private int defId;

	private MatIdManager(int entriesNb, int entriesSize) {
		this.entriesSize = entriesSize;
		tableData = BufferUtils.createByteBuffer(entriesNb * entriesSize * 4);
		//TODO 3.1 : use new Image(this.nativeFormat.get(), this.pWidth, this.pHeight, this.jmeData, com.jme3.texture.image.ColorSpace.sRGB);
		tableImage = new Image(Format.RGBA8, entriesSize, entriesNb, tableData);
		tableTex = new Texture2D(tableImage);
		tableTex.setWrap(WrapMode.Clamp);
		tableTex.setMinFilter(MinFilter.NearestNoMipMaps);
		tableTex.setMagFilter(MagFilter.Nearest);
		defId = findMatId(ColorRGBA.BlackNoAlpha, ColorRGBA.BlackNoAlpha);
	}

	public MatIdManager() {
		this(256, 2);
	}

	public int findMatId(ColorRGBA diffuse, ColorRGBA specular) {
		if (diffuse == null || specular == null) return defId;
		//TODO search for already existing MatId
		int id = nextId;
		nextId++;
		tableData.position(id * entriesSize * 4);
		tableData.put(diffuse.asBytesRGBA());
		tableData.put(specular.asBytesRGBA());
		return id;
	}

	public int size() {
		return nextId;
	}

}
