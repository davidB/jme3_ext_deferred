#import "ShaderLib/Deferred.glsllib"

in vec2 texCoord;

uniform sampler2D m_DiffuseData;
uniform sampler2D m_SpecularData;
uniform sampler2D m_NormalData;
uniform sampler2D m_DepthData;

uniform vec4 g_LightColor;
uniform vec4 g_LightPosition;
uniform vec3 g_CameraPosition;

uniform mat4 m_ViewProjectionMatrixInverse;

float lightComputeDiffuse(in vec3 norm, in vec3 lightdir, in vec3 viewdir){
	return max(0.0, dot(norm, lightdir));
}

float lightComputeSpecular(in vec3 norm, in vec3 viewdir, in vec3 lightdir, in float shiny){
       // Blinn-Phong
       // Note: preferably, H should be computed in the vertex shader
       vec3 H = (viewdir + lightdir) * vec3(0.5);
       return pow(max(dot(H, norm), 0.0), shiny);
}

vec2 computeLighting(in vec3 wvPos, in vec3 wvNorm, in vec3 wvViewDir, in vec4 wvLightDir, in float shiny){
   float diffuseFactor  = lightComputeDiffuse(wvNorm, wvLightDir.xyz, wvViewDir);
   float specularFactor = lightComputeSpecular(wvNorm, wvViewDir, wvLightDir.xyz, shiny);
   return vec2(diffuseFactor, specularFactor) * vec2(wvLightDir.w);
}

vec3 getPosition(in sampler2D depthData, in vec2 newTexCoord){
  //Reconstruction from depth
  float depth = texture2D(depthData, newTexCoord).r;
  vec4 pos;
  pos.xy = (newTexCoord * vec2(2.0)) - vec2(1.0);
  pos.z  = depth;
  pos.w  = 1.0;
  pos    = m_ViewProjectionMatrixInverse * pos;
  //pos   /= pos.w;
  return pos.xyz;
}

// JME3 lights in world space
void lightComputeDir(in vec3 worldPos, in vec4 color, in vec4 position, out vec4 lightDir){
        lightDir.xyz = position.xyz - worldPos.xyz;
        float dist = length(lightDir.xyz);
        lightDir.w = clamp(1.0 - position.w * dist, 0.0, 1.0);
        lightDir.xyz /= dist;
}


out vec4 out_FragColor;

void main(){
    vec2 newTexCoord = texCoord;
    vec4 diffuseColor = texture2D(m_DiffuseData,  newTexCoord);
    //if (diffuseColor.a == 0.0)
    //    discard;
/*
    vec4 specularColor = texture2D(m_SpecularData, newTexCoord);
    vec3 worldPosition = getPosition(m_DepthData, newTexCoord);
    vec3 viewDir  = normalize(g_CameraPosition - worldPosition);

    vec4 intNormal = texture2D(m_NormalData, newTexCoord);
    vec3 normal = decodeNormal(intNormal.rgb);


    vec4 lightDir;
    lightComputeDir(worldPosition, g_LightColor, g_LightPosition, lightDir);

    vec2 light = computeLighting(worldPosition, normal, viewDir, lightDir, specularColor.w*128.0);

    out_FragColor = vec4(light.x * diffuseColor.xyz + light.y * specularColor.xyz, 1.0);
    out_FragColor.xyz *= g_LightColor.xyz;
*/
    out_FragColor.rgb = vec3(0.0, 1.0, 0.0);
}

