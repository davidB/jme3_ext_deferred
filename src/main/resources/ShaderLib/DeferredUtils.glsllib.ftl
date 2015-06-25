<#include "UnitVector.glsllib.ftl">

vec3 encodeNormal(in vec3 normal){
 return snorm12x2_to_unorm8x3(float32x3_to_oct(normal));
}

vec3 decodeNormal(in vec3 unorm8x3Normal){
 return oct_to_float32x3(unorm8x3_to_snorm12x2(unorm8x3Normal));
 //return hemioct_to_float32x3(unorm8x3_to_snorm12x2(intNormal.rgb));
}

vec3 readNormal(in sampler2D normalBuffer, in vec2 uv) {
	vec3 intNormal = texture(normalBuffer, uv).rgb;
	return normalize(decodeNormal(intNormal));
}

// Reconstructs screen-space non-unit normal from screen-space position
// @src G3D
vec3 reconstructNonUnitCSFaceNormal(vec3 C) {
	return cross(dFdy(C), dFdx(C));
}

// Reconstructs screen-space unit normal from screen-space position
// @src G3D
vec3 reconstructCSFaceNormal(vec3 C) {
	return normalize(cross(dFdy(C), dFdx(C)));
}

// @return [0,1] hyperbolic value stored in depthBuffer
float readRawDepth(in sampler2D depthBuffer, in vec2 uv) {
    return texture(depthBuffer, uv).r;
}

// @see http://www.geeks3d.com/20091216/geexlab-how-to-visualize-the-depth-buffer-in-glsl/
// [Depth testing](file:///Users/davidb/Library/Application%20Support/Firefox/Profiles/osjconli.default/ScrapBook/data/20150621222102/index.html)
// @return [0,1] linearized value stored in depthBuffer
float readDepth(in sampler2D depthBuffer, in vec2 uv, float near, float far) {
    float z = readRawDepth(depthBuffer, uv) * 2.0 - 1.0; //back to NDC
    return (2.0 * near) / (far + near - z * (far - near));
}

vec3 readDiffuse(in sampler2D diffuseBuffer, in vec2 uv) {
	return texture(diffuseBuffer, uv).rgb;
}


// Clipping plane constants for use by reconstructZ
// @param clipInfo = (far == -inf()) ? Vector3(near, -1.0f, 1.0f) : Vector3(near * far,  near - far,  far);
// @return [-near, -far]
// @src G3D
float reconstructCSZ(float d, vec3 clipInfo) {
    return clipInfo[0] / (clipInfo[1] * d + clipInfo[2]);
}

float reconstructCSZ(float d, float n, float f) {
	vec3 clipInfo = vec3(n * f, n - f, f);
    return reconstructCSZ(d, clipInfo);
    //d = (2.0 * n) / (f + n - d * (f - n));
    //return -1 * (near + (d * far - near));
    //float zndc = d * 2.0 - 1.0;
	// conversion into eye space
	//return 2*f*n / (zndc*(f-n)-(f+n));
}

// Reconstruct camera-space P.xyz from screen-space S = (x, y) in
// pixels and camera-space z < 0.  Assumes that the upper-left pixel center
// is at (0.5, 0.5) [but that need not be the location at which the sample tap
// was placed!]
//
// Costs 3 MADD.  Error is on the order of 10^3 at the far plane, partly due to z precision.
//
// projInfo = vec4(-2.0f / (width*P[0][0]),
//          -2.0f / (height*P[1][1]),
//          ( 1.0f - P[0][2]) / P[0][0],
//          ( 1.0f + P[1][2]) / P[1][1])
//
//    where P is the projection matrix that maps camera space points
//    to [-1, 1] x [-1, 1].  That is, Camera::getProjectUnit().
// @src G3D
vec3 reconstructCSPosition(vec2 S, float z, vec4 projInfo) {
    return vec3((S.xy * projInfo.xy + projInfo.zw) * z, z);
}

// Helper for reconstructing camera-space P.xyz from screen-space S = (x, y) in
// pixels and hyperbolic depth.
// @src G3D
vec3 reconstructCSPositionFromDepth(vec2 S, float depth, vec4 projInfo, vec3 clipInfo) {
    return reconstructCSPosition(S, reconstructCSZ(depth, clipInfo), projInfo);
}

// Helper for the common idiom of getting world-space position P.xyz from screen-space S = (x, y) in
// pixels and hyperbolic depth.
// @src G3D
vec3 reconstructWSPositionFromDepth(vec2 S, float depth, vec4 projInfo, vec3 clipInfo, mat4 cameraToWorld) {
    return (cameraToWorld * vec4(reconstructCSPositionFromDepth(S, depth, projInfo, clipInfo), 1.0)).xyz;
}


