#import "ShaderLib/SpacesConverters.glsllib"
uniform mat4 g_ViewProjectionMatrixInverse;
uniform mat4 g_ProjectionMatrixInverse;
uniform mat3 m_RotationViewMatrix;
#ifdef IN_MINIGBUFFER
uniform sampler2D m_MiniGBuffer;
#else
uniform sampler2D m_DepthBuffer;
uniform sampler2D m_NormalBuffer;
#endif
uniform vec2 m_ResHigh;
uniform vec2 g_FrustumNearFar;
uniform vec4 g_ViewPort;
uniform mat4 g_ProjectionMatrix;

in vec2 texCoord;

out vec4 fragData;

const int lod = 0;

vec3 getPosition(float rawDepth, ivec2 posSS, vec2 res) {
  //return ES_reconstructPosition(rawDepth, posSS, res, g_FrustumNearFar, g_ViewPort);
  return ES_reconstructPosition(rawDepth, posSS, res, g_ProjectionMatrixInverse);
}
//vec3 getPositionWS(ivec2 posSS) {
//    //float depth = readRawDepth(m_DepthBuffer, posSS / g_Resolution);
//    float depth = linearizeDepth(texelFetch(m_DepthBuffer, posSS, lod).r);
//    //return reconstructWSPositionFromDepth(posSS + vec2(0.5), depth, m_ProjInfo, m_ClipInfo, g_ViewMatrixInverse);
//    //return vec3(0.0);
//    //vec4 pos = vec4((vec2(posSS) + vec2(0.5)) / textureSize(m_DepthBuffer, lod), depth, 1.0) * 2.0 - 1.0;
//    //pos = g_ViewProjectionMatrixInverse * pos;
//    //return pos.xyz / pos.w;
//}

void main(){
  //vec2 texCoord = vTexCoord;

  //vec2 xy2 = gl_FragCoord.xy;

  //vec2 resHigh = textureSize(m_DepthBuffer, lod);
  vec2 resHigh = m_ResHigh;
  ivec2 xyHigh = ivec2(texCoord * resHigh); //previous buffer is double size

  vec4 data[4];
  vec3 pos[4];
  
#ifdef IN_MINIGBUFFER
  data[0] = texelFetch(m_MiniGBuffer, xyHigh + ivec2(-1,  -1), lod);
  data[1] = texelFetch(m_MiniGBuffer, xyHigh + ivec2(1,  -1), lod);
  data[2] = texelFetch(m_MiniGBuffer, xyHigh + ivec2(1,  1), lod);
  data[3] = texelFetch(m_MiniGBuffer, xyHigh + ivec2(-1,  1), lod);
#else
  data[0] = texelFetch(m_DepthBuffer, xyHigh + ivec2(-1,  -1), lod).rrrr;
  data[1] = texelFetch(m_DepthBuffer, xyHigh + ivec2(1,  -1), lod).rrrr;
  data[2] = texelFetch(m_DepthBuffer, xyHigh + ivec2(1,  1), lod).rrrr;
  data[3] = texelFetch(m_DepthBuffer, xyHigh + ivec2(-1,  1), lod).rrrr;
  
  vec3 norm[4];
  norm[0] = texelFetch(m_NormalBuffer, xyHigh + ivec2(-1, -1), lod).xyz;
  norm[1] = texelFetch(m_NormalBuffer, xyHigh + ivec2(1,  -1), lod).xyz;
  norm[2] = texelFetch(m_NormalBuffer, xyHigh + ivec2(1,  1), lod).xyz;
  norm[3] = texelFetch(m_NormalBuffer, xyHigh + ivec2(-1,  1), lod).xyz;
#endif

  pos[0] = getPosition(data[0].a, xyHigh + ivec2(0,  0), resHigh);
  pos[1] = getPosition(data[1].a, xyHigh + ivec2(1,  0), resHigh);
  pos[2] = getPosition(data[2].a, xyHigh + ivec2(1,  1), resHigh);
  pos[3] = getPosition(data[3].a, xyHigh + ivec2(0,  1), resHigh);
  //depth[3] = texture(m_DepthBuffer, near(texCoord, m_Resolution.xy, vec2(-0.5, -0.5))).r;

//norm[0] = texture(m_NormalBuffer, texCoord).xyz;
  //norm[3] = texture(m_NormalBuffer, near(texCoord, m_Resolution.xy, vec2(-0.5, -0.5))).xyz;

  //float maxZ = max(max(depth[0]., depth[1]), max(depth[2], depth[3]));
  //float minZ = min(min(depth[0], depth[1]), min(depth[2], depth[3]));
  float maxZ = max(max(pos[0].z, pos[1].z), max(pos[2].z, pos[3].z));
  float minZ = min(min(pos[0].z, pos[1].z), min(pos[2].z, pos[3].z));


//  int minIdx, maxIdx;

//  for (int i = 0; i < 4; ++i)
//  {
//    if (depth[i] == minZ)
//      minIdx = i;
//    if (depth[i] == maxZ)
//      maxIdx = i;
//  }

  int minPos, maxPos;

  for (int i = 0; i < 4; ++i)
  {
    if (pos[i].z == minZ)
      minPos = i;
    if (pos[i].z == maxZ)
      maxPos = i;
  }

  ivec2 median = ivec2(0, 0);
  int index = 0;

//  for (int i = 0; i < 4 && index < 2; ++i)
//    if (i != minIdx && i != maxIdx)
//      median[index++] = i;
  for (int i = 0; i < 4 && index < 2; ++i)
    if (i != minPos && i != maxPos)
      median[index++] = i;

  float depthOut;
  vec3 posOut;
  vec3 normalOut;
  float d = distance(pos[minPos].xyz, pos[maxPos].xyz);
  if (d < 1.0) {
    //depthOut = (depth[median.x] + depth[median.y]) / 2.0;
    posOut = (pos[median.x] + pos[median.y]) / 2.0;
#ifdef IN_MINIGBUFFER
    normalOut = normalize((data[median.x].xyz + data[median.y].xyz) / 2.0);
#else
    normalOut = (decodeNormal(norm[median.x]) + decodeNormal(norm[median.y])) / 2.0;
    normalOut = normalize(m_RotationViewMatrix * normalOut);
#endif
  } else {
    posOut = pos[median.x];
#ifdef IN_MINIGBUFFER
    normalOut = data[median.x].xyz;
#else
    normalOut = decodeNormal(norm[median.x]);
    normalOut = normalize(m_RotationViewMatrix * normalOut);
#endif
  }
  //normalOut *= sign(normalOut.z); // front face == positive z in ES
  

	//normalOut = norm[0];
	//float depth = fragDepth(posOut, g_ProjectionMatrix, g_FrustumNearFar);
	float depth = ES_reconstructDepth(posOut.z, g_FrustumNearFar.x, g_FrustumNearFar.y);
	//float depth = 0.5;
	//float depth = texture(m_DepthBuffer, texCoord).r;
    //fragData[1] = vec4(encodeNormal(normalize(normalOut)), 1.0);
    //fragData = vec4(normalize(normalOut), 1.0);
    //fragData = vec4(texCoord, 0.0, 1.0);
    //fragData = vec4(-posOut.z/g_FrustumNearFar.y, 1.0, 1.0, 1.0);
    //fragData = vec3(depth, 0.0, 0.0);
	//gl_FragDepth = depthOut;
	//gl_FragDepth = depth;
	//fragData2 = vec3(depth, 0.0, 0.0);
	//fragData[0] = vec4(0.4, 0.0, 0.0, 1.0);
	fragData = vec4(normalize(normalOut), depth);
}
