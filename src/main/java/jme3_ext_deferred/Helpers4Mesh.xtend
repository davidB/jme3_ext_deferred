package jme3_ext_deferred

import java.nio.IntBuffer
import com.jme3.math.FastMath
import com.jme3.math.Vector3f
import com.jme3.scene.Mesh
import com.jme3.scene.Mesh.Mode
import com.jme3.scene.VertexBuffer.Type
import com.jme3.util.BufferUtils

class Helpers4Mesh {
	/** 
	 * Fill index with basic tessellation of a convex polygon of vertices sorted.
	 * The tessellation algo is very basic : every triangles start from the point 0 + [pointsOffset]
	 */
	def static void tessellation0(IntBuffer index, boolean cw, int... vertices) {
		var int nbTriangles = vertices.length - 2

		for (var int t = 0; t < nbTriangles; t++) {
			index.put(vertices.get(0))
			if (cw) {
				index.put({
					val _rdIndx_vertices = t + 1
					vertices.get(_rdIndx_vertices)
				})
				index.put({
					val _rdIndx_vertices = t + 2
					vertices.get(_rdIndx_vertices)
				})
			} else {
				index.put({
					val _rdIndx_vertices = t + 2
					vertices.get(_rdIndx_vertices)
				})
				index.put({
					val _rdIndx_vertices = t + 1
					vertices.get(_rdIndx_vertices)
				})
			}
		}

	}

	def static void tessellation0(IntBuffer index, boolean cw, boolean loop, int verticesBegin, int verticesEnd) {
		var int lg = Math::abs(verticesEnd - verticesBegin) + 1
		var int inc = Math::signum(verticesEnd - verticesBegin) as int
		var int[] vertices = newIntArrayOfSize(lg + (if(loop) 1 else 0 ))

		for (var int i = 0; i < lg; i++) {
			{
				val _wrVal_vertices = vertices
				val _wrIndx_vertices = i
				_wrVal_vertices.set(_wrIndx_vertices, verticesBegin + inc * i)
			}
		}
		if (loop) {
			{
				val _wrVal_vertices = vertices
				val _wrIndx_vertices = lg
				_wrVal_vertices.set(_wrIndx_vertices, verticesBegin + inc)
			}
		}
		tessellation0(index, cw, vertices)
	}

	def static Mesh newCone(int samples, float rangeY, float bottomRadius) {
		var Vector3f[] positions = newArrayOfSize(samples + 1)
		var IntBuffer index = BufferUtils::createIntBuffer((samples + 1 + samples) * 3)
		{
			val _wrVal_positions = positions
			_wrVal_positions.set(0, new Vector3f(0, 0, 0))
		}
		var int bottomOffset = 1
		var float stepSize = FastMath::TWO_PI / samples

		for (var int i = 0; i < samples; i++) {
			{
				val _wrVal_positions = positions
				val _wrIndx_positions = bottomOffset + i
				_wrVal_positions.set(_wrIndx_positions,
					new Vector3f(FastMath::cos(stepSize * i) * bottomRadius, rangeY, FastMath::sin(stepSize * i) *
						bottomRadius))
			}
		}
		// side faces
		tessellation0(index, false, true, 0, bottomOffset + samples - 1) // bottom faces
		tessellation0(index, true, false, bottomOffset, bottomOffset + samples - 1)
		var Mesh m = new Mesh()
		m.setBuffer(Type::Position, 3, BufferUtils::createFloatBuffer(positions))
		m.setBuffer(Type::Index, 3, index) // m.setPointSize(3);
		m.setMode(Mode::Triangles)
		m.updateBound()
		return m
	}

}
