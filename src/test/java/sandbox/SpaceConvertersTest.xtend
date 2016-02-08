package sandbox

import org.junit.Test
import com.jme3.math.Vector3f
import com.jme3.math.Matrix4f
import com.jme3.math.Vector4f
import org.junit.Assert
import com.jme3.renderer.Camera
import com.jme3.math.FastMath
import static org.hamcrest.CoreMatchers.*
import static org.hamcrest.number.OrderingComparison.*

class SpaceConvertersTest {
/*
vec3 PS_fromES(vec3 posES, mat4 projectionMatrix) {
        return (projectionMatrix * vec4(posES, 1.0)).xyz;
}

vec3 ES_fromPS(vec3 posPS, mat4 projectionMatrixInverse) {
        vec4 p = (projectionMatrixInverse * vec4(posPS, 1.0));
        return p.xyz / p.w;
}

// https://mynameismjp.wordpress.com/2009/03/10/reconstructing-position-from-depth/
vec3 ES_reconstructPosition(float rawDepth, ivec2 posSS, vec2 res, mat4 projectionMatrixInverse) {
    // Get x/w and y/w from the viewport position
    float xNDC = toNDC(posSS.x/res.x);
    //float y = (1 - posSS.y/res.y) * 2 - 1;
    float yNDC = toNDC(posSS.y/res.y);
    vec3 posPS = vec3(xNDC, yNDC, rawDepth);
    return ES_fromPS(posPS, projectionMatrixInverse);
} 
 */
    def Vector3f PS_fromES(Vector3f posES, Matrix4f projectionMatrix) {
       val v4 = projectionMatrix.mult(new Vector4f(posES.x, posES.y, posES.z, 1.0f))
       new Vector3f(v4.x/v4.w, v4.y/v4.w, v4.z/v4.w);
    }
    
    def Vector3f ES_fromPS(Vector3f posPS, Matrix4f projectionMatrixInverse) {
       val v4 = projectionMatrixInverse.mult(new Vector4f(posPS.x, posPS.y, posPS.z, 1.0f))
       new Vector3f(v4.x / v4.w, v4.y/v4.w, v4.z/v4.w);
    }

    @Test
    def testES_to_PS() {
        //val projectionMatrix = new Matrix4f()
        val projectionMatrix = new Matrix4f(
             1.3579952f,  0.0f,  0.0f,  0.0f, 
             0.0f,  2.4142134f,  0.0f,  0.0f, 
             0.0f,  0.0f,  -1.002002f,  -2.002002f, 
             0.0f,  0.0f,  -1.0f,  -0.0f 
        )
        val projectionMatrixInverse = projectionMatrix.invert()
        val posES0 = new Vector3f(0, 1, -2)
        Assert.assertEquals(posES0, ES_fromPS(PS_fromES(posES0, projectionMatrix), projectionMatrixInverse)) 
    }
 
    // http://www.derschmale.com/2014/01/26/reconstructing-positions-from-the-depth-buffer/
    @Test
    def testES_ReconstructingFromPerspective() {
        val projectionMatrix = new Matrix4f(
             1.3579952f,  0.0f,  0.0f,  0.0f, 
             0.0f,  2.4142134f,  0.0f,  0.0f, 
             0.0f,  0.0f,  -1.002002f,  -2.002002f, 
             0.0f,  0.0f,  -1.0f,  -0.0f 
        )
        val rays = makeCornerRays(projectionMatrix)
    }
    
    def makeCornerRays(Matrix4f projectionMatrix) {
        // You could set z = -1.0f instead of 0.0f for OpenGL
        // but it doesn't matter since any z value lies on the same ray anyway.
        val  homogenousCorners = #[
            new Vector4f(-1.0f, -1.0f, -1.0f, 1.0f),            
            new Vector4f(1.0f, -1.0f, -1.0f, 1.0f),
            new Vector4f(1.0f, 1.0f, -1.0f, 1.0f),
            new Vector4f(-1.0f, 1.0f, -1.0f, 1.0f)
        ]
        val projectionMatrixInverse = projectionMatrix.invert()
        val rays = <Vector4f>newArrayOfSize(4)
        for (var i = 0; i < 4; i++) {
            // unproject the frustum corner from NDC to view space
            val ray = projectionMatrixInverse.mult(homogenousCorners.get(i))
            ray.multLocal( 1f / ray.w)
            ray.multLocal( 1f / ray.z)
            // z-normalize this vector
            rays.set(i, ray);
        }
        rays
    }
    
    def ES_zFromDepth(float rawDepth, Matrix4f projectionMatrix){
        val zNDC = rawDepth * 2f - 1f
        val zES = -1f * projectionMatrix.m32 / (zNDC + projectionMatrix.m22)
        zES
    }
    
    def ES_reconstructPosFromDepth(float rawDepth, Matrix4f projectionMatrix, Vector4f[] rayCorners, float u, float v) {
        val zES = ES_zFromDepth(rawDepth, projectionMatrix)
        val viewDir = interpolate(rayCorners, u, v)
        viewDir.multLocal(zES/viewDir.z)
    }
    
    // see http://www.derschmale.com/2014/09/28/unprojections-explained/
    // @param posNDC is position Homogeneous (xNDC, yNDC, zNDC, 1.0)
    def ES_reconstructPosFromNDC(Vector4f posNDC, Matrix4f projectionMatrixInverse) {
        val v4 = projectionMatrixInverse.mult(posNDC)
        new Vector3f(v4.x / v4.w, v4.y / v4.w, v4.z/v4.w)     
    }
    
    def interpolate(Vector4f[] rayCorners, float u, float v) {
        val vX0 = mix(rayCorners.get(0), rayCorners.get(1), u)
        val vX1 = mix(rayCorners.get(3), rayCorners.get(2), u)
        mix(vX0, vX1, v)
    }
    
    def mix(Vector4f v0, Vector4f v1, float ratio) {
        v0.mult(1f - ratio).add(v1.mult(ratio))
    }
    
    def mix(float v0, float v1, float ratio) {
        v0 * (1f - ratio) +  v1 * ratio
    }
    
    @Test
    def testInterpolate() {
        val  rayCorners = #[
            new Vector4f(-1.0f, -1.0f, -1.0f, 1.0f),            
            new Vector4f(1.0f, -1.0f, -1.0f, 1.0f),
            new Vector4f(1.0f, 1.0f, -1.0f, 1.0f),
            new Vector4f(-1.0f, 1.0f, -1.0f, 1.0f)
        ]
        Assert.assertEquals(rayCorners.get(0), interpolate(rayCorners, 0, 0))
        Assert.assertEquals(rayCorners.get(1), interpolate(rayCorners, 1, 0))
        Assert.assertEquals(rayCorners.get(2), interpolate(rayCorners, 1, 1))
        Assert.assertEquals(rayCorners.get(3), interpolate(rayCorners, 0, 1))
        Assert.assertEquals(new Vector4f(0f, 0f, -1.0f, 1.0f), interpolate(rayCorners, 0.5f, 0.5f))
    }
    
    @Test
    def testES_reconstructPosFromNDC_OnCorner() {
        val cam = new Camera(16 * 50, 9 * 50)
        cam.setFrustumPerspective(FastMath.PI / 2f, 16f/9f, 0.1f, 100f)
        val projectionMatrixInverse = cam.projectionMatrix.invert
        
        val nearPrecisionSq = cam.frustumNear * 0.0001f;  //TODO used a squared distance
        assertDistanceSquared(new Vector3f(cam.frustumLeft, cam.frustumBottom, -cam.frustumNear), ES_reconstructPosFromNDC(new Vector4f(-1, -1, -1, 1), projectionMatrixInverse), nearPrecisionSq)
        assertDistanceSquared(new Vector3f(cam.frustumLeft, cam.frustumTop, -cam.frustumNear), ES_reconstructPosFromNDC(new Vector4f(-1, 1, -1, 1), projectionMatrixInverse), nearPrecisionSq)
        assertDistanceSquared(new Vector3f(cam.frustumRight, cam.frustumTop, -cam.frustumNear), ES_reconstructPosFromNDC(new Vector4f(1, 1, -1, 1), projectionMatrixInverse), nearPrecisionSq)
        assertDistanceSquared(new Vector3f(cam.frustumRight, cam.frustumBottom, -cam.frustumNear), ES_reconstructPosFromNDC(new Vector4f(1, -1, -1, 1), projectionMatrixInverse), nearPrecisionSq)

        assertDistanceSquared(new Vector3f(0.0f, 0.0f, -cam.frustumNear), ES_reconstructPosFromNDC(new Vector4f(0, 0, -1, 1), projectionMatrixInverse), nearPrecisionSq)

        val farPrecisionSq = cam.frustumFar * 0.1f; //TODO used a squared distance
        assertDistanceSquared(new Vector3f(cam.frustumLeft, cam.frustumBottom, -cam.frustumFar), ES_reconstructPosFromNDC(new Vector4f(-1, -1, 1, 1), projectionMatrixInverse), farPrecisionSq)
        assertDistanceSquared(new Vector3f(cam.frustumLeft, cam.frustumTop, -cam.frustumFar), ES_reconstructPosFromNDC(new Vector4f(-1, 1, 1, 1), projectionMatrixInverse), farPrecisionSq)
        assertDistanceSquared(new Vector3f(cam.frustumRight, cam.frustumTop, -cam.frustumFar), ES_reconstructPosFromNDC(new Vector4f(1, 1, 1, 1), projectionMatrixInverse), farPrecisionSq)
        assertDistanceSquared(new Vector3f(cam.frustumRight, cam.frustumBottom, -cam.frustumFar), ES_reconstructPosFromNDC(new Vector4f(1, 1, 1, 1), projectionMatrixInverse), farPrecisionSq)
    }
    
    def assertDistanceSquared(Vector3f expected, Vector3f actual, float distanceSquaredMax) {
        Assert.assertThat(expected.distanceSquared(actual), is(lessThanOrEqualTo(distanceSquaredMax)))
    }
    
    @Test
    def testZfromDepth() {
        val cam = new Camera(16 * 50, 9 * 50)
        val n = 0.1f
        val f = 100f
        cam.setFrustumPerspective(FastMath.PI / 2f, 16f/9f, n, f)
        //val projectionMatrixInverse = cam.projectionMatrix.invert
        val v0 = new Vector4f(5f, 6f, 7f, 1f)
        val zp = (7f * (n+f) + 2 * f *n)/(n -f)
        Assert.assertThat(zp, is(cam.projectionMatrix.mult(v0).z))        
        Assert.assertThat(v0.z,  is(-1f * cam.projectionMatrix.mult(v0).w))        
    }
}