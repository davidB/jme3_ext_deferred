package jme3_ext_deferred;

import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.renderer.Camera;

public final class Helpers {
	public final static Vector4f projInfo(Camera cam, int w, int h) {
		Matrix4f pm = cam.getProjectionMatrix();
		return new Vector4f(
			-2.0f / (w * pm.m00),
			-2.0f / (h * pm.m11),
			( 1.0f - pm.m02) / pm.m00,
			( 1.0f - pm.m12) / pm.m11
			);
	}

	public final static double projScale(Camera cam, int w, int h) {
		Matrix4f pm = cam.getProjectionMatrix();
		return -0.5 * h * pm.m11;
	}

	/**
	 * @return clipInfo = (far == -inf()) ? Vector3(near, -1.0f, 1.0f) : Vector3(near * far,  near - far,  far)
	 */
	public final static Vector3f clipInfo(Camera cam) {
		float near = cam.getFrustumNear();
		float far = cam.getFrustumFar();
		return (far == Float.NEGATIVE_INFINITY) ? new Vector3f(near, -1.0f, 1.0f) : new Vector3f(near * far,  near - far,  far);
	}
}