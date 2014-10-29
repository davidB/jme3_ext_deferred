#import "ShaderLib/DeferredUtils.glsllib"

uniform vec4 m_Color;
uniform vec3 m_Position;

uniform sampler2D m_DepthBuffer;
uniform sampler2D m_NormalBuffer;
uniform sampler2D m_MatBuffer;
uniform vec4 m_ProjInfo;
uniform vec3 m_ClipInfo;
//ViewProjectionMatrixInverse
uniform vec2 g_FrustumNearFar;
uniform mat4 g_ViewMatrixInverse;
uniform vec2 g_Resolution;
uniform vec3 g_CameraPosition;

out vec4 out_FragColor;

const float PI = 3.14159265358979323846264;

vec3 getWSPosition(vec2 posSS) {
	float depth = readRawDepth(m_DepthBuffer, posSS / g_Resolution);
	return reconstructWSPositionFromDepth(posSS + vec2(0.5), depth, m_ProjInfo, m_ClipInfo, g_ViewMatrixInverse);
	//return vec3(0.0);
}


float attenuation(vec3 dir){
  float dist = length(dir);
  float radiance = 1.0/(1.0+pow(dist/10.0, 2.0));
  return clamp(radiance*10.0, 0.0, 1.0);
}

float influence(vec3 normal, float coneAngle){
  float minConeAngle = ((360.0-coneAngle-10.0)/360.0)*PI;
  float maxConeAngle = ((360.0-coneAngle)/360.0)*PI;
  return smoothstep(minConeAngle, maxConeAngle, acos(normal.z));
}

float lambert(vec3 surfaceNormal, vec3 lightDir){
  return max(0.0, dot(surfaceNormal, lightDir));
}

void main(){
	vec2 posSS = gl_FragCoord.xy;
	vec2 texCoord = posSS / g_Resolution;
	vec3 posWS = getWSPosition(posSS);
	vec3 norWS = readNormal(m_NormalBuffer, texCoord);
	int matId = int(texelFetch(m_NormalBuffer, ivec2(posSS), 0).a * 256.0);
	vec4 diffuse = texelFetch(m_MatBuffer, ivec2(0, matId), 0);
	vec4 specular = texelFetch(m_MatBuffer, ivec2(1, matId), 0);
	float shininess = 0.5;
	//vec3 diffuse = readDiffuse(m_DiffuseBuffer, texCoord);
	vec3 lightDirWS = normalize(posWS - m_Position);

	//vec3 lPosition = (lightView * vVertex).xyz;
	//vec3 lightPosNormal = normalize(lPosition);
	//vec3 lightSurfaceNormal = lightRot * normal;
//	float lighting = (
//		lambert(norWS, normalize(posWS - m_Position)) *
//		//influence(lightPosNormal, lightConeAngle) *
//		//attenuation(lPosition) *
//		//shadowOf(lPosition)
//		1.0
//	);
	float intensity = max(dot(norWS,lightDirWS), 0.0);
	vec4 spec = vec4(0.0);
    if (intensity > 0.0) {
        vec3 h = normalize(lightDirWS + g_CameraPosition);
        float intSpec = max(dot(h,norWS), 0.0);
        spec = specular * pow(intSpec, shininess);
    }

	vec4 intensityColor = m_Color * intensity;
	//out_FragColor = max(intensity * diffuse + spec, 0.0);
	out_FragColor = max(intensityColor * diffuse + spec, 0.0);
	//out_FragColor.rgb = posWS;
	//out_FragColor.rgb = vec3(float(matId) * 10.0 /256.0);
	//out_FragColor.a = 1.0;
}
