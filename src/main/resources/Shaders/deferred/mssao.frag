#import "ShaderLib/DeferredUtils.glsllib"

uniform sampler2D m_DepthBuffer;
uniform sampler2D m_NormalBuffer;
uniform vec4 m_ProjInfo;
uniform float m_ProjScale;

uniform vec2 g_FrustumNearFar;
uniform vec2 g_Resolution;
uniform mat4 g_ViewMatrix;
uniform float g_Time;
uniform mat4 g_ProjectionMatrix;
uniform mat4 g_ViewProjectionMatrixInverse;

uniform float m_dMax;
uniform float m_rMax;
uniform float m_r;

uniform sampler2D m_loResAOTex;
uniform sampler2D m_loResNormTex;
uniform sampler2D m_loResDepthTex;

#ifdef LAST
uniform sampler2D m_lastFrameAOTex;
uniform sampler2D m_lastFramePosTex;
uniform mat4 m_iMVMat;
uniform mat4 m_mVMat;
uniform float m_poissonDisk[32];
#endif

in vec2 texCoord;
out vec4 fragData;

const int lod = 0;

vec3 getPositionWS(ivec2 posSS) {
	//float depth = readRawDepth(m_DepthBuffer, posSS / g_Resolution);
	float depth = texelFetch(m_DepthBuffer, posSS, lod).r;
	//return reconstructWSPositionFromDepth(posSS + vec2(0.5), depth, m_ProjInfo, m_ClipInfo, g_ViewMatrixInverse);
	//return vec3(0.0);
	vec4 pos = vec4((vec2(posSS) + vec2(0.5)) / textureSize(m_DepthBuffer, lod), depth, 1.0) * 2.0 - 1.0;
	pos = g_ViewProjectionMatrixInverse * pos;
	return pos.xyz / pos.w;

}

vec3 getNormalWS(ivec2 posSS) {
  return decodeNormal(texelFetch(m_NormalBuffer, posSS, lod).xyz);
}

void computeOcclusion(vec3 p, vec3 n, ivec2 posSS, inout float occlusion, inout float sampleCount){
  vec3 samplePos = getPositionWS(posSS);
  vec3 sampleNorm = getNormalWS(posSS);
  float d = distance(p.xyz, samplePos.xyz);  
  float t = min(1.0, (d * d) / (m_dMax * m_dMax));
#ifdef LAST
  t = mix(1.0, 0.0, t);
#else
  t = 1.0 - t;
#endif  
  vec3 diff = normalize(samplePos.xyz - p.xyz);  
  float cosTheta = max(dot(n, diff), 0.0);  
  occlusion += t * cosTheta; // * sampleNorm.w$;     
  sampleCount += 1.0;    
}

vec3 Upsample(ivec2 posSS, vec3 n, vec3 p) {
  ivec2 loResCoord[4];
  //loResCoord[0] = floor((posSS + vec2(-1.0,  1.0)) / 2.0);// + vec2(0.5, 0.5);    
  //loResCoord[1] = floor((posSS + vec2( 1.0,  1.0)) / 2.0);// + vec2(0.5, 0.5);
  //loResCoord[2] = floor((posSS + vec2(-1.0, -1.0)) / 2.0);// + vec2(0.5, 0.5);
  //loResCoord[3] = floor((posSS + vec2( 1.0, -1.0)) / 2.0);// + vec2(0.5, 0.5);
  ivec2 pos0 = posSS / 2;
  loResCoord[0] = pos0 + ivec2(-1,  1);    
  loResCoord[1] = pos0 + ivec2( 1,  1);
  loResCoord[2] = pos0 + ivec2(-1, -1);
  loResCoord[3] = pos0 + ivec2( 1, -1);    
  vec3 loResAO[4];
  vec3 loResNorm[4];
  float loResDepth[4];
  for (int i = 0; i < 4; ++i) {
    loResNorm[i] = getNormalWS(loResCoord[i]);
    loResDepth[i] = texelFetch(m_loResDepthTex, loResCoord[i], lod).r; //texture2DRect(loResPosTex, loResCoord[i]).z;
    loResAO[i] = texelFetch(m_loResAOTex, loResCoord[i], lod).xyz;
  }
  float normWeight[4];
  for (int i = 0; i < 4; ++i) {    
    normWeight[i] = (dot(loResNorm[i], n) + 1.1) / 2.1;
    normWeight[i] = pow(normWeight[i], 8.0);
  }
  float depthWeight[4];
  for (int i = 0; i < 4; ++i) {
    depthWeight[i] = 1.0 / (1.0 + abs(p.z - loResDepth[i]) * 0.2);
    depthWeight[i] = pow(depthWeight[i], 16.0);
  }
  float totalWeight = 0.0;
  vec3 combinedAO = vec3(0.0);
  for (int i = 0; i < 4; ++i) {
    float weight = normWeight[i] * depthWeight[i] * (9.0 / 16.0) /
      (abs((gl_FragCoord.x - loResCoord[i].x * 2.0) * (gl_FragCoord.y - loResCoord[i].y * 2.0)) * 4.0);    
    totalWeight += weight;
    combinedAO += loResAO[i] * weight;    
  }
  combinedAO /= totalWeight;  
  return combinedAO;
}

vec4 mssao(ivec2 posSS){
  vec3 p = getPositionWS(posSS);
  vec3 n = getNormalWS(posSS);
  float occlusion = 0.0;

  float rangeMax = min(m_r / abs(p.z), m_rMax);    

#ifdef LAST
  float sampleCount = 0.0;
  for (int i = 0; i < 32; i += 2){     
    ComputeOcclusion(posSS + vec2(m_poissonDisk[i], m_poissonDisk[i + 1]) * rangeMax, occlusion, sampleCount);    
  }
  vec3 upsample = Upsample(posSS, n, p);
  float aoMax = max(upsample.x, occlusion / sampleCount);  
  float aoAverage = (upsample.y + occlusion) / (upsample.z + sampleCount);

  float currentFrameAO = (1.0 - aoMax) * (1.0 - aoAverage); 
  /*
  vec4 lastFrameEyePos = mVMat * (iMVMat * p);
  vec4 lastFrameScreenPos = g_ProjectionMatrix * lastFrameEyePos;
  vec2 lastFrameTexCoord = lastFrameScreenPos.xy / lastFrameScreenPos.w;
  lastFrameTexCoord = floor((lastFrameTexCoord + 1.0) * resolution / 2.0) + vec2(0.5, 0.5);
  float lastFrameZ = texelFetch(m_lastFramePosTex, ivec2(lastFrameTexCoord), lod).z;
  float lastFrameWeight = 0.0;
  if (abs(1.0 - lastFrameZ / lastFrameEyePos.z) < 0.01)
    lastFrameWeight = 0.6;
  if (lastFrameTexCoord.x < 0.0 || lastFrameTexCoord.x > g_Resolution.x ||
      lastFrameTexCoord.y < 0.0 || lastFrameTexCoord.y > g_Resolution.y)
    lastFrameWeight = 0.0;
  float lastFrameAO = texelFetch(m_lastFrameAOTex, lastFrameTexCoord).x;
  */
  float lastFrameWeight = 0.6;
  float lastFrameAO = texelFetch(m_lastFrameAOTex, posSS/2, lod).x;
  return vec4(lastFrameAO * lastFrameWeight + currentFrameAO * (1.0 - lastFrameWeight));

#else
  float sampleCount = 0.0001;

  for (float x = 1.0; x <= rangeMax; x += 2.0) {
    for (float y = 1.0; y <= rangeMax; y += 2.0) {
      computeOcclusion(p, n, posSS + ivec2(x, y), occlusion, sampleCount);
      computeOcclusion(p, n, posSS + ivec2(-x, y), occlusion, sampleCount);
      computeOcclusion(p, n, posSS + ivec2(-x, -y), occlusion, sampleCount);
      computeOcclusion(p, n, posSS + ivec2(x, -y), occlusion, sampleCount);
    }
  }

#ifdef MIDDLE
  vec3 upsample = Upsample(posSS, n, p);
  return vec4(max(upsample.x, occlusion / sampleCount), upsample.y + occlusion, upsample.z + sampleCount, 0.0);
  //return vec3(0.0, 1.0, 0.0); 
#else
  //return vec4(occlusion / sampleCount, occlusion, sampleCount, 0.0);
  //return vec4(1.0, 0.0, 0.0, 0.0);
  return vec4(p, 1.0);
#endif
#endif
}

void main() {
//ivec2 posSS = ivec2(gl_FragCoord.xy
	ivec2 posSS = ivec2(texCoord * textureSize(m_NormalBuffer, lod)+ vec2(0.5));
	fragData = mssao(posSS); 
}
