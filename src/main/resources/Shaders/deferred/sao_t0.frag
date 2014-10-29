#import "ShaderLib/DeferredUtils.glsllib"

uniform sampler2D m_DepthBuffer;
uniform sampler2D m_NormalBuffer;
uniform vec3 m_FrustumCorner;
uniform vec2 g_FrustumNearFar;
uniform float m_Bias;
uniform float m_Intensity;
uniform mat4 m_ViewProjectionMatrixInverse;

in vec2 texCoord;

out vec4 out_FragColor;

uniform vec2 g_Resolution;
uniform vec3 g_CameraPosition;
uniform mat4 g_ViewMatrix;
uniform mat4 g_WorldViewMatrix;
uniform mat4 g_ProjectionMatrix;
uniform mat4 g_ProjectionMatrixInverse;

uniform sampler2D m_RandomMap;
uniform float m_SampleRadius;
uniform vec2 m_Scale;
uniform vec2[4] m_Samples;

#define PI 3.14116
#define PIx2 6.28
#define USE_GBUFFER_NORMALS 0
#define NUM_SAMPLES 9
#define NUM_SPIRAL_TURNS 7
#define VARIATION 1

// return linerarized [0,1]
//float readDepth(in vec2 uv) {
//	return readDepth(m_DepthBuffer, uv, g_FrustumNearFar.x, g_FrustumNearFar.y);
//}


// Note that positions (which may affect z) are snapped during rasterization, but
// attributes are not.

/*
 Clipping plane constants for use by reconstructZ

 \param clipInfo = (z_f == -inf()) ? Vector3(z_n, -1.0f, 1.0f) : Vector3(z_n * z_f,  z_n - z_f,  z_f);
 \sa G3D::Projection::reconstructFromDepthClipInfo
*/
float reconstructCSZ(float d, vec3 clipInfo) {
    return clipInfo[0] / (clipInfo[1] * d + clipInfo[2]);
}

/** Reconstruct camera-space P.xyz from screen-space S = (x, y) in
    pixels and camera-space z < 0.  Assumes that the upper-left pixel center
    is at (0.5, 0.5) [but that need not be the location at which the sample tap
    was placed!]

    Costs 3 MADD.  Error is on the order of 10^3 at the far plane, partly due to z precision.

 projInfo = vec4(-2.0f / (width*P[0][0]),
          -2.0f / (height*P[1][1]),
          ( 1.0f - P[0][2]) / P[0][0],
          ( 1.0f + P[1][2]) / P[1][1])

    where P is the projection matrix that maps camera space points
    to [-1, 1] x [-1, 1].  That is, Camera::getProjectUnit().

    \sa G3D::Projection::reconstructFromDepthProjInfo
*/
vec3 reconstructCSPosition(vec2 S, float z, vec4 projInfo) {
    return vec3((S.xy * projInfo.xy + projInfo.zw) * z, z);
}

/** Helper for reconstructing camera-space P.xyz from screen-space S = (x, y) in
    pixels and hyperbolic depth.

    \sa G3D::Projection::reconstructFromDepthClipInfo
    \sa G3D::Projection::reconstructFromDepthProjInfo
*/
vec3 reconstructCSPositionFromDepth(vec2 S, float depth, vec4 projInfo, vec3 clipInfo) {
    return reconstructCSPosition(S, reconstructCSZ(depth, clipInfo), projInfo);
}

vec3 reconstructCSPositionFromDepth(vec2 uv) {
    vec2 S = uv * g_Resolution;
	vec3 clipInfo = vec3(g_FrustumNearFar.x * g_FrustumNearFar.y, g_FrustumNearFar.x - g_FrustumNearFar.y, g_FrustumNearFar.y);
	float depth = readRawDepth(m_DepthBuffer, uv);
	//float depth = texelFetch(m_DepthBuffer, S, 0).r;
	vec4 projInfo = vec4(
		-2.0f / (g_Resolution.x * g_ProjectionMatrix[0][0]),
		-2.0f / (g_Resolution.y * g_ProjectionMatrix[1][1]),
		( 1.0f - g_ProjectionMatrix[0][2]) / g_ProjectionMatrix[0][0],
		( 1.0f - g_ProjectionMatrix[1][2]) / g_ProjectionMatrix[1][1]
	);
    return reconstructCSPositionFromDepth(S + vec2(0.5), depth, projInfo, clipInfo);

    //vec4 position = vec4(uv*2.0-1.0, depth*2.0-1.0, 1.0);
    //position = g_ProjectionMatrixInverse * position;
    //return position.xyz/position.w;
}

/** Reconstructs screen-space unit normal from screen-space position */
vec3 reconstructCSFaceNormal(vec3 C) {
    return normalize(cross(dFdy(C), dFdx(C)));
}

vec3 reconstructNonUnitCSFaceNormal(vec3 C) {
    return cross(dFdy(C), dFdx(C));
}

/*
vec3 getPositionVS(in vec2 uv, in mat4 viewProjectionInverseMatrix, vec3 cameraPositionWS, mat4 viewMatrix) {
	float depth = -1.0 * readDepth(uv);
	vec2 uv2 = uv * 2.0 - vec2(1.0);
	vec4 temp = viewProjectionInverseMatrix * vec4(uv2, -1.0, 1.0);
	vec3 cameraFarPlaneWS = (temp / temp.w).xyz;

	vec3 cameraToPositionRay = normalize(cameraFarPlaneWS - cameraPositionWS);
	vec3 originWS = cameraToPositionRay * depth + cameraPositionWS;
	return (viewMatrix * vec4(originWS, 1.0)).xyz;

	//TODO try return viewMatrix * getPositionWS
}

vec3 getPositionVS(in vec2 uv) {
	return getPositionVS(uv, m_ViewProjectionMatrixInverse, g_CameraPosition, g_ViewMatrix);
}
*/

// Returns a unit vector and a screen-space radius for the tap on a unit disk
// (the caller should scale by the actual disk radius)
vec2 tapLocation(int sampleNumber, float spinAngle, out float radiusSS){
    // Radius relative to radiusSS
    float alpha = float(sampleNumber + 0.5) * (1.0 / float(NUM_SAMPLES));
    float angle = alpha * (NUM_SPIRAL_TURNS * PIx2) + spinAngle;

    radiusSS = alpha;
    return vec2(cos(angle), sin(angle));
}

vec3 getOffsetCSPosition(vec2 uv, vec2 unitOffset, float radiusSS) {
	uv = uv + radiusSS * unitOffset * (1.0 / g_Resolution);
	return reconstructCSPositionFromDepth(uv);
}
float sampleAO(vec2 uv, vec3 positionVS, vec3 normalVS, float sampleRadiusSS, int tapIndex, float rotationAngle, float radiusWS2, float bias) {
	const float epsilon = 0.01;
	float radiusSS;
	vec2 unitOffset = tapLocation(tapIndex, rotationAngle, radiusSS);
	radiusSS *= sampleRadiusSS;

	vec3 Q = getOffsetCSPosition(uv, unitOffset, radiusSS);
	vec3 v = Q - positionVS;

	float vv = dot(v, v);
	float vn = dot(v, normalVS) - bias;
#if  VARIATION == 0
	// (from HPG12 paper)
	// Note large epsilon to avoid overdarkening within cracks
	return float(vv < radiusWS2) * max(vn / (epsilon + vv), 0.0);
#elif VARIATION == 1 //default - recommended
	// ssmoother transition to zero (lowers contrast, smoothing out corners).
	float f = max(radiusWS2 - vv, 0.0) / radiusWS2;
	return f * f * f * max (vn / (epsilon + vv), 0.0);
#elif VARIATION == 2
	// Medium contrast (which looks better at high radii), no division.
	// Note that the contribution still falls off with radius^2, but we've adjusted the rate in a way
	// that is more computationally efficient and happens to be aesthetically pleasing.
	float invRadiusWS2 = 1.0 / radiusWS2;
	return 4.0 * max (1.0 - vv * invRadiusWS2, 0.0) * max(vn, 0.0);
#else
	// low contrast, no division operation
	return 2.0 * float(vv < radiusWS2) * max(vn, 0.0);
#endif
}

float occlusion_sao(in vec2 uv, sampler2D normalBuffer, sampler2D noiseBuffer, vec2 noiseScale, float radiusWS, float intensity, float bias, float fov) {
	vec3 originCS = reconstructCSPositionFromDepth(uv);
#if 	USE_GBUFFER_NORMALS
	vec3 normalWS = readNormal(normalBuffer, uv);
	vec3 normalCS = (g_WorldViewMatrix * vec4(normalWS,1.0)).xyz;
#else
	vec3 normalCS = reconstructCSFaceNormal(originCS);
#endif
	vec3 sampleNoise = texture2D(noiseBuffer, uv * noiseScale).xyz;
	float randomPatternRotationAngle = 2.0 * PI *sampleNoise.x;

	float occlusion = 0.0;

	//FIXME remove hard coded value
	float projScale = 40.0; // 1.0 / (2.0 * tan(fov * 0.5));
	float radiusSS = projScale * radiusWS / originCS.y; // radius of influence in screen space
	float radiusWS2 = radiusWS * radiusWS;
	for(int i = 0; i < NUM_SAMPLES; ++i) {
		occlusion += sampleAO(uv, originCS, normalCS, radiusSS, i, randomPatternRotationAngle, radiusWS2, bias);
	}
	occlusion = 1.0 - occlusion / (4.0 * float(NUM_SAMPLES));
	occlusion = clamp(pow(occlusion, 1.0 + intensity), 0.0, 1.0);
	return occlusion;
}

void main(){
	float occlusion = occlusion_sao(texCoord, m_NormalBuffer, m_RandomMap, m_Scale, m_SampleRadius,m_Intensity, m_Bias, 90);
	out_FragColor = vec4(vec3(occlusion), 1.0);
	vec3 originCS = reconstructCSPositionFromDepth(texCoord);
	vec3 normalWS = readNormal(m_NormalBuffer, texCoord);
	//vec3 normalCS = (g_WorldViewMatrix * vec4(normalWS,1.0)).xyz;
	vec3 normalCS = reconstructCSFaceNormal(originCS);
	//out_FragColor.rgb = originCS;
}
