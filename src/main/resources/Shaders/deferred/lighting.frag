#import "ShaderLib/DeferredUtils.glsllib"
#import "ShaderLib/PhysicallyBasedLighting.glsllib"

uniform vec4 m_Color;
uniform vec3 m_LightPos;
uniform vec3 m_LightDir;
uniform float m_LightFallOffDist;

uniform sampler2D m_DepthBuffer;
uniform sampler2D m_NormalBuffer;
uniform sampler2D m_MatBuffer;
uniform vec4 m_ProjInfo;
uniform vec3 m_ClipInfo;
//ViewProjectionMatrixInverse
uniform vec2 g_FrustumNearFar;
uniform mat4 g_ViewMatrixInverse;
uniform mat4 g_ViewProjectionMatrixInverse;
uniform vec2 g_Resolution;
uniform vec3 g_CameraPosition;

out vec4 out_FragColor;

//const float PI = 3.14159265358979323846264;

vec3 getWSPosition(vec2 posSS) {
	float depth = readRawDepth(m_DepthBuffer, posSS / g_Resolution);
	//return reconstructWSPositionFromDepth(posSS + vec2(0.5), depth, m_ProjInfo, m_ClipInfo, g_ViewMatrixInverse);
	//return vec3(0.0);
	vec4 pos = vec4(posSS / g_Resolution, depth, 1.0) * 2.0 - 1.0;
	pos = g_ViewProjectionMatrixInverse * pos;
	return pos.xyz / pos.w;

}

// * http://imdoingitwrong.wordpress.com/2011/01/31/light-attenuation/
// * http://wiki.blender.org/index.php/Doc:FR/2.6/Manual/Lighting/Lights/Light_Attenuation
// * http://blog.selfshadow.com/publications/s2013-shading-course/karis/s2013_pbs_epic_notes_v2.pdf p12
// * http://gamedev.stackexchange.com/questions/51291/deferred-rendering-and-point-light-radius
// * http://gamedev.stackexchange.com/questions/56897/glsl-light-attenuation-color-and-intensity-formula
// * http://gamedev.stackexchange.com/questions/64149/what-light-attenuation-function-does-udk-use
float attenuation(float dist, float falloffDist){
  //float dist = length(dir);
  //float radiance = 1.0 - ((dist * dist) / (falloffDist * falloffDist));
  //float radiance = 1.0 - (dist  / falloffDist);
  //radiance *= 1 / (dist * dist);
  // I = E × (D2 / (D2 + Q × r2)) from http://wiki.blender.org/index.php/Doc:FR/2.6/Manual/Lighting/Lights/Light_Attenuation
  //float D2 = (falloffDist * 0.25) * (falloffDist * 0.25);
  //float Q = 1.0;
  //float radiance = D2 / (D2 + r2);
  // from http://blog.selfshadow.com/publications/s2013-shading-course/karis/s2013_pbs_epic_notes_v2.pdf p12
  //float d2 = dist * dist;
  //float dr = pow(dist / falloffDist, 4) ;
  //float n = clamp(1-dr, 0.0, 1.0);
  //return (n*n) / (d2 + 1);
  //return n;
  //return clamp(radiance, 0.0, 1.0);
  //float radiance = 1.0/(1.0+pow(dist/falloffDist, 2.0));
  //float radiance = 1.0/(1.0+pow(dist/falloffDist, 2.0));
  float radiance = 1.0/(pow(1.0+ dist*4/falloffDist, 2.0));
  //return clamp(radiance*falloffDist, 0.0, 1.0);
  return clamp(radiance, 0.0, 1.0);
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
	//int matId = int(texelFetch(m_NormalBuffer, ivec2(posSS), 0).a * 256.0);
	//vec4 diffuse = texelFetch(m_MatBuffer, ivec2(0, matId), 0);
	//vec4 specular = texelFetch(m_MatBuffer, ivec2(1, matId), 0);
	float shininess = 0.5;
#ifdef WSLIGHTDIR
	vec3 lightDirWS = normalize(-m_LightDir);
#else
	vec3 lightDirWS = normalize(m_LightPos - posWS);
#endif

	vec3 viewDir = normalize(g_CameraPosition - posWS);
	//vec3 lightColor = m_Color.rgb;
	vec3 lightColor = m_Color.rgb;//vec3(1.0);
	float spec = 0.03;
	float gloss = 512;
	vec3 outDiffuse = vec3(0);
	float outSpecular = 0;
	PBR_ComputeDirectLight(norWS, lightDirWS, viewDir, lightColor, spec, gloss, QUALITY_HIGH, outDiffuse, outSpecular);

#ifdef FALLOFF
	outDiffuse *= attenuation(distance(m_LightPos, posWS), m_LightFallOffDist);
	//outDiffuse *= influence(normalize(m_LightPos), 20);
#endif

	out_FragColor.rgb = outDiffuse;
	out_FragColor.a = outSpecular;
}
