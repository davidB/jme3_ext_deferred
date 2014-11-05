package jme3_ext_deferred;

import java.nio.IntBuffer;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;

public class Helpers4Mesh {

	/**
	 * Fill index with basic tessellation of a convex polygon of vertices sorted.
	 * The tessellation algo is very basic : every triangles start from the point 0 + [pointsOffset]
	 */
	public static void tessellation0(IntBuffer index, boolean cw, int... vertices) {
		int nbTriangles = vertices.length - 2;
		for(int t = 0; t < nbTriangles; t++) {
			index.put(vertices[0]);
			if (cw) {
				index.put(vertices[t+1]);
				index.put(vertices[t+2]);
			} else {
				index.put(vertices[t+2]);
				index.put(vertices[t+1]);
			}
		}
	}

	public static void tessellation0(IntBuffer index, boolean cw, boolean loop, int verticesBegin, int verticesEnd) {
		int lg = Math.abs(verticesEnd - verticesBegin) + 1;
		int inc = (int)Math.signum(verticesEnd - verticesBegin);
		int[] vertices = new int[lg + (loop? 1: 0)];
		for(int i = 0; i < lg; i++) {
			vertices[i] = verticesBegin + inc * i;
		}
		if (loop) {
			vertices[lg] = verticesBegin + inc;
		}
		tessellation0(index, cw, vertices);
	}

	public static Mesh newCone(int samples, float rangeY, float bottomRadius) {
		Vector3f[] positions = new Vector3f[samples + 1];
		IntBuffer index = BufferUtils.createIntBuffer((samples+1 + samples ) * 3);
		positions[0] = new Vector3f(0,0,0);
		int bottomOffset = 1;
		float stepSize = FastMath.TWO_PI / samples;
		for (int i = 0; i < samples; i++) {
			positions[bottomOffset + i] = new Vector3f(FastMath.cos(stepSize * i) * bottomRadius, rangeY, FastMath.sin(stepSize * i) * bottomRadius);
		}
		// side faces
		tessellation0(index, false, true, 0, bottomOffset + samples - 1);
		// bottom faces
		tessellation0(index, true, false, bottomOffset, bottomOffset + samples - 1);
		Mesh m = new Mesh();
		m.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(positions));
		m.setBuffer(Type.Index, 3, index);
		//m.setPointSize(3);
		m.setMode(Mode.Triangles);
		m.updateBound();
		return m;
	}

}
