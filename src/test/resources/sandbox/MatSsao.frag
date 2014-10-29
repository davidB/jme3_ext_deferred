#import "ShaderLib/DeferredUtils.glsllib"

uniform sampler2D m_DepthBuffer;
uniform sampler2D m_NormalBuffer;
uniform vec3 m_FrustumCorner;
uniform vec2 g_FrustumNearFar;
uniform float m_Bias;
uniform float m_Intensity;

in vec2 texCoord;

out vec4 out_FragColor;
#import "Common/ShaderLib/MultiSample.glsllib"

uniform vec2 g_Resolution;
uniform sampler2D m_RandomMap;
uniform float m_SampleRadius;
uniform vec2 m_Scale;
uniform vec2[4] m_Samples;

vec3 getPosition(in vec2 uv, float csz, vec3 frustumCorner){
  //one frustum corner method
  float x = mix(-frustumCorner.x, frustumCorner.x, uv.x);
  float y = mix(-frustumCorner.y, frustumCorner.y, uv.y);

  return csz * vec3(x, y, frustumCorner.z);
}

vec2 getRandom(in sampler2D map, in vec2 uv){
   //float rand=(fract(uv.x*(g_Resolution.x/2.0))*0.25)+(fract(uv.y*(g_Resolution.y/2.0))*0.5);
   vec4 rand=texture2D(map,g_Resolution * uv / 128.0 * 3.0)*2.0 -1.0;

   return normalize(rand.xy);
}

float doAmbientOcclusion(sampler2D depthBuffer, in vec2 uv, in vec3 pos, in vec3 norm, float near, float far, vec3 frustumCorner, float bias, float intensity){
   float csz = readDepth(depthBuffer, uv, near, far);
   vec3 diff = getPosition(uv, csz, frustumCorner)- pos;
   vec3 v = normalize(diff);
   float d = length(diff) * m_Scale.x;

   return max(0.0, dot(norm, v) - bias) * ( 1.0/(1.0 + d) ) * intensity;
}


vec2 reflection(in vec2 v1,in vec2 v2){
    vec2 result= 2.0 * dot(v2, v1) * v2;
    result=v1-result;
    return result;
}


void main0(){

   float result;

	float csz = readDepth(m_DepthBuffer, texCoord, g_FrustumNearFar.x, g_FrustumNearFar.y);
   vec3 position = getPosition(texCoord, csz, m_FrustumCorner);
    //optimization, do not calculate AO if depth is 1
   if(csz==1.0){
        out_FragColor=vec4(1.0);
        return;
   }
   vec3 normal = readNormal(m_NormalBuffer, texCoord);
   vec2 rand = getRandom(m_RandomMap, texCoord);

   float ao = 0.0;
   float rad =m_SampleRadius / position.z;


   int iterations = 4;
   for (int j = 0; j < iterations; ++j){
      vec2 coord1 = reflection(vec2(m_Samples[j]), rand) * vec2(rad,rad);
      vec2 coord2 = vec2(coord1.x* 0.707 - coord1.y* 0.707, coord1.x* 0.707 + coord1.y* 0.707) ;

      ao += doAmbientOcclusion(m_DepthBuffer, texCoord + coord1.xy * 0.25, position, normal, g_FrustumNearFar.x, g_FrustumNearFar.y, m_FrustumCorner, m_Bias, m_Intensity);
      ao += doAmbientOcclusion(m_DepthBuffer, texCoord + coord2 * 0.50, position, normal, g_FrustumNearFar.x, g_FrustumNearFar.y, m_FrustumCorner, m_Bias, m_Intensity);
      ao += doAmbientOcclusion(m_DepthBuffer, texCoord + coord1.xy * 0.75, position, normal, g_FrustumNearFar.x, g_FrustumNearFar.y, m_FrustumCorner, m_Bias, m_Intensity);
      ao += doAmbientOcclusion(m_DepthBuffer, texCoord + coord2 * 1.00, position, normal, g_FrustumNearFar.x, g_FrustumNearFar.y, m_FrustumCorner, m_Bias, m_Intensity);

   }
   ao /= float(iterations) * 4.0;
   result = 1.0-ao;

   out_FragColor=vec4(result,result,result, 1.0);

}

void main1(){
	float csz = readDepth(m_DepthBuffer, texCoord, g_FrustumNearFar.x, g_FrustumNearFar.y);
	out_FragColor = vec4(getPosition(texCoord, csz, m_FrustumCorner), 1.0);
}

void main(){
  main0();
}

