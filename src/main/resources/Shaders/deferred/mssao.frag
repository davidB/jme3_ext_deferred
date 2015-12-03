#import "ShaderLib/SpacesConverters.glsllib"

uniform vec2 g_FrustumNearFar;
uniform vec2 g_Resolution;
uniform mat4 g_ViewMatrix;
uniform float g_Time;
uniform mat4 g_ProjectionMatrix;
uniform mat4 g_ProjectionMatrixInverse;
uniform mat4 g_ViewProjectionMatrixInverse;
uniform vec4 g_ViewPort;
uniform vec2 m_ResHigh;
uniform float m_dMax;
uniform float m_rMax;
uniform float m_r;

uniform sampler2D m_loResAOTex;
uniform sampler2D m_loResMiniGBuffer;

#ifdef IN_MINIGBUFFER
uniform sampler2D m_MiniGBuffer;
#else
uniform sampler2D m_DepthBuffer;
uniform sampler2D m_NormalBuffer;
#endif

#ifdef LAST
uniform sampler2D m_lastFrameAOTex;
uniform sampler2D m_lastFramePosTex;
uniform float m_poissonDisk[32];
#endif

in vec2 texCoord;
out vec4 fragData;

const int lod = 0;

struct Point {
  vec3 pos;
  vec3 normal;
  float depth; // for debug
};

Point getPoint(ivec2 posSS, vec2 res) {
    Point p;
#ifdef IN_MINIGBUFFER
    vec4 data = texelFetch(m_MiniGBuffer, posSS, lod);
    p.depth = data.a;
    //p.pos = ES_reconstructPosition(p.depth, posSS, res, g_FrustumNearFar, g_ViewPort);
    p.pos = ES_reconstructPosition(p.depth, posSS, res, g_ProjectionMatrixInverse);
    
    p.normal = normalize(data.xyz);
#else
    p.depth = texelFetch(m_DepthBuffer, posSS, lod).r;
    p.pos = ES_reconstructPosition(p.depth, posSS, res, g_FrustumNearFar, g_ViewPort);  
    p.normal = decodeNormal(texelFetch(m_NormalBuffer, posSS, lod).xyz);
    //p.normal = normalize(mat3(g_ViewMatrix) * p.normal);
#endif
  //  p.normal *= sign(p.normal.z); // front face == positive z in ES
    return p;
}

vec2 computeOcclusion(vec3 p, vec3 n, ivec2 posSS, in float occlusion, in float sampleCount, vec2 res){
  Point sample = getPoint(posSS, res);
  float d = distance(p.xyz, sample.pos.xyz);  
  float t = 1.0 -  min(1.0, (d * d) / (m_dMax * m_dMax));
  vec3 diff = normalize(sample.pos.xyz - p.xyz);  
  float cosTheta = max(dot(n, diff), 0.0);  
  return vec2(
    occlusion + t * cosTheta,// * sign(abs(sample.pos.z)), 
    sampleCount + 1.0
  );
}

vec3 Upsample(ivec2 posSS, vec3 n, vec3 p) {
  ivec2 loResCoord[4];
  //loResCoord[0] = ivec2(floor((posSS + vec2(-1.0,  1.0)) / 2.0) + vec2(0.5, 0.5));    
  //loResCoord[1] = ivec2(floor((posSS + vec2( 1.0,  1.0)) / 2.0) + vec2(0.5, 0.5));
  //loResCoord[2] = ivec2(floor((posSS + vec2(-1.0, -1.0)) / 2.0) + vec2(0.5, 0.5));
  //loResCoord[3] = ivec2(floor((posSS + vec2( 1.0, -1.0)) / 2.0) + vec2(0.5, 0.5));
  ivec2 pos0 = posSS / 2;
  loResCoord[0] = pos0 + ivec2(-1,  1);    
  loResCoord[1] = pos0 + ivec2( 1,  1);
  loResCoord[2] = pos0 + ivec2(-1, -1);
  loResCoord[3] = pos0 + ivec2( 1, -1);    
  vec3 loResAO[4];
  vec3 loResNorm[4];
  float loResZ[4];
  for (int i = 0; i < 4; ++i) {
    vec4 loData = texelFetch(m_loResMiniGBuffer, loResCoord[i], lod);
    loResNorm[i] = loData.xyz;
    loResZ[i] = ES_reconstructZ(loData.a, g_FrustumNearFar.x, g_FrustumNearFar.y);
    loResAO[i] = texelFetch(m_loResAOTex, loResCoord[i], lod).xyz;
  }
  float normWeight[4];
  for (int i = 0; i < 4; ++i) {    
    normWeight[i] = (dot(loResNorm[i], n) + 1.1) / 2.1;
    normWeight[i] = pow(normWeight[i], 8.0); // 8.0 is the tolerance on normal
  }
  float depthWeight[4];
  for (int i = 0; i < 4; ++i) {
    depthWeight[i] = 1.0 / (1.0 + abs(p.z - loResZ[i]) * 0.2);
    depthWeight[i] = pow(depthWeight[i], 16.0); // 16.0 is the tolerance on z
  }
  float totalWeight = 0.0;
  vec3 combinedAO = vec3(0.0);
  for (int i = 0; i < 4; ++i) {
    float weight = normWeight[i] * depthWeight[i] * (9.0 / 16.0) /
      (abs((posSS.x - loResCoord[i].x * 2.0) * (posSS.y - loResCoord[i].y * 2.0)) * 4.0);    
    totalWeight += weight;
    combinedAO += loResAO[i] * weight;    
  }
  combinedAO /= totalWeight;  
  return combinedAO;
}

vec4 mssao(ivec2 posSS, vec2 res){
  Point point = getPoint(posSS, res);
  vec3 p = point.pos;
  vec3 n = point.normal;
  float occlusion = 0.0;

  float rangeMax = min(m_r / abs(p.z), m_rMax);    
  //rangeMax = 3.0;
#ifdef LAST
  float sampleCount = 0.0;
  vec2 aoNear = vec2(occlusion, sampleCount);
  for (int i = 0; i < 32; i += 2){     
    aoNear = computeOcclusion(p, n, posSS + ivec2(vec2(m_poissonDisk[i], m_poissonDisk[i + 1]) * rangeMax), aoNear.x, aoNear.y, res);    
  }
  occlusion = aoNear.x;
  sampleCount = aoNear.y;
  vec3 aoFar = Upsample(posSS, n, p);
  float aoMax = max(aoFar.x, occlusion / sampleCount);  
  float aoAverage = (aoFar.y + occlusion) / (aoFar.z + sampleCount);

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
  float lastFrameWeight = 0.0;
  float lastFrameAO = 0.0;
  //return vec4(currentFrameAO, 0.0, 0.0, 1.0);
  return vec4(lastFrameAO * lastFrameWeight + currentFrameAO * (1.0 - lastFrameWeight), 0.0, 0.0, 1.0);

#else
  float sampleCount = 0.0001;
  vec2 aoNear = vec2(occlusion, sampleCount);
  for (int x = 1; x <= rangeMax; x += 2) {
    for (int y = 1; y <= rangeMax; y += 2) {
      aoNear = computeOcclusion(p, n, posSS + ivec2(x, y), aoNear.x, aoNear.y, res);
      aoNear = computeOcclusion(p, n, posSS + ivec2(-x, y), aoNear.x, aoNear.y, res);
      aoNear = computeOcclusion(p, n, posSS + ivec2(-x, -y), aoNear.x, aoNear.y, res);
      aoNear = computeOcclusion(p, n, posSS + ivec2(x, -y), aoNear.x, aoNear.y, res);
    }
  }
  occlusion = aoNear.x;
  sampleCount = aoNear.y;
#ifdef MIDDLE
  vec3 aoFar = Upsample(posSS, n, p);
  //aoFar = vec3(0.0);
  return vec4(max(aoFar.x, occlusion / sampleCount), aoFar.y + occlusion, aoFar.z + sampleCount, 1.0);
#else
  return vec4(occlusion / sampleCount, occlusion, sampleCount, 1.0);
#endif
#endif
}

void main() {
//ivec2 posSS = ivec2(gl_FragCoord.xy
#ifdef IN_MINIGBUFFER
    vec2 res = textureSize(m_MiniGBuffer, lod);
#else
    vec2 res = textureSize(m_NormalBuffer, lod);
#endif
res = m_ResHigh;
	ivec2 posSS = ivec2(texCoord * res);
	//fragData = vec4(texCoord, 0.0, 1.0);
	fragData = mssao(posSS, res); 
	//Point data = getPoint(posSS, res);
	//fragData = vec4(-data.pos.z/g_FrustumNearFar.y, 0,0,1.0);
}
