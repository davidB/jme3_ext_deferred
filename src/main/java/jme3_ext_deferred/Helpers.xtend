package jme3_ext_deferred

import com.jme3.math.Vector3f
import com.jme3.math.Vector4f
import com.jme3.renderer.Camera

final class Helpers {
	def final static Vector4f projInfo(Camera cam, int w, int h) {
		val pm = cam.getProjectionMatrix()
		new Vector4f(-2.0f / (w * pm.m00), -2.0f / (h * pm.m11), (1.0f - pm.m02) / pm.m00, (1.0f - pm.m12) / pm.m11)
	}

	def final static double projScale(Camera cam, int w, int h) {
		val pm = cam.getProjectionMatrix()
		return -0.5 * h * pm.m11
	}

	/** 
	 * @return clipInfo = (far == -inf()) ? Vector3(near, -1.0f, 1.0f) : Vector3(near * far,  near - far,  far)
	 */
	def final static Vector3f clipInfo(Camera cam) {
		val near = cam.getFrustumNear()
		val far = cam.getFrustumFar()
		if (far === Float::NEGATIVE_INFINITY) {
			new Vector3f(near, -1.0f, 1.0f)
		} else {
			new Vector3f(near * far, near - far, far)
		}
	}

}
