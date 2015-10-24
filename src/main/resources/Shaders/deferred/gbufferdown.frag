#import "ShaderLib/DeferredUtils.glsllib"
uniform mat4 g_ViewProjectionMatrixInverse;
uniform sampler2D m_DepthBuffer;
uniform sampler2D m_NormalBuffer;

in vec2 texCoord;

out vec4 fragData;

const int lod = 0;

vec3 getPositionCS(ivec2 posSS) {
    float depth = texelFetch(m_DepthBuffer, posSS, lod).r;
    return reconstructCSPositionFromDepth(depth, m_ProjInfo, m_ClipInfo, g_ViewMatrixInverse);
}
vec3 getPositionWS(ivec2 posSS) {
    //float depth = readRawDepth(m_DepthBuffer, posSS / g_Resolution);
    float depth = linearizeDepth(texelFetch(m_DepthBuffer, posSS, lod).r);
    //return reconstructWSPositionFromDepth(posSS + vec2(0.5), depth, m_ProjInfo, m_ClipInfo, g_ViewMatrixInverse);
    //return vec3(0.0);
    //vec4 pos = vec4((vec2(posSS) + vec2(0.5)) / textureSize(m_DepthBuffer, lod), depth, 1.0) * 2.0 - 1.0;
    //pos = g_ViewProjectionMatrixInverse * pos;
    //return pos.xyz / pos.w;
}

void main(){
  //vec2 texCoord = vTexCoord;

  //vec2 xy2 = gl_FragCoord.xy;

  ivec2 xyHigh = ivec2(texCoord * textureSize(m_DepthBuffer, lod)); //previous buffer is double size
  
  vec3 pos[4];
  vec3 norm[4];
 
  pos[0] = getPositionWS(xyHigh + ivec2(0,  0));
  pos[1] = getPositionWS(xyHigh + ivec2(1,  0));
  pos[2] = getPositionWS(xyHigh + ivec2(1,  1));
  pos[3] = getPositionWS(xyHigh + ivec2(0,  1));
  //depth[3] = texture(m_DepthBuffer, near(texCoord, m_Resolution.xy, vec2(-0.5, -0.5))).r;

//norm[0] = texture(m_NormalBuffer, texCoord).xyz;
  norm[0] = texelFetch(m_NormalBuffer, xyHigh + ivec2(0,  0), lod).xyz;
  norm[1] = texelFetch(m_NormalBuffer, xyHigh + ivec2(1,  0), lod).xyz;
  norm[2] = texelFetch(m_NormalBuffer, xyHigh + ivec2(1,  1), lod).xyz;
  norm[3] = texelFetch(m_NormalBuffer, xyHigh + ivec2(0,  1), lod).xyz;
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
    normalOut = (decodeNormal(norm[median.x]) + decodeNormal(norm[median.y])) / 2.0;
  } else {
    posOut = pos[median.x];
    normalOut = norm[median.x];
  }

	//normalOut = norm[0];
	//depthOut = depth[0];
    fragData = vec4(encodeNormal(normalize(normalOut)), 1.0);
	//gl_FragDepth = depthOut;
	gl_FragDepth = posOut.z;
}
