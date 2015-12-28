#import "ShaderLib/SpacesConverters.glsllib"

uniform vec2 g_FrustumNearFar;
uniform mat4 g_ProjectionMatrixInverse;
uniform mat3 m_RotationViewMatrix;

uniform sampler2D m_AOTex;
uniform sampler2D m_MiniGBuffer;
uniform vec2 m_ResHigh;

in vec2 texCoord;
out vec4 fragData;

struct Point {
  vec3 pos;
  vec3 normal;
  float depth; // for debug
};

const int lod = 0;

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
    p.normal = normalize(m_RotationViewMatrix * p.normal);
#endif
  //  p.normal *= sign(p.normal.z); // front face == positive z in ES
    return p;
}

void main()
{
  ivec2 posSS = ivec2(texCoord * m_ResHigh);
  Point point = getPoint(posSS, m_ResHigh);
  vec3 p = point.pos;
  vec3 n = point.normal;
  
  vec3 ss = vec3(0.0);

  float weight = 0.0;

  for (float i = -1.0; i <= 1.0; i += 1.0) {
    for (float j = -1.0; j <= 1.0; j += 1.0) {
      ivec2 xy = posSS + ivec2(i, j);
      vec4 data = texelFetch(m_MiniGBuffer, xy, lod);
      vec3 norm = data.xyz;
      float z = ES_reconstructZ(data.a, g_FrustumNearFar.x, g_FrustumNearFar.y);
      vec3 t = texelFetch(m_AOTex, xy, lod).xyz;  
      
      float normWeight = (dot(norm, n) + 1.2) / 2.2;
      normWeight = pow(normWeight, 8.0);

      float depthWeight = 1.0 / (1.0 + abs(p.z - z) * 0.2);
      depthWeight = pow(depthWeight, 16.0);

      float gaussianWeight = 1.0 / ((abs(i) + 1.0) * (abs(j) + 1.0));

      float w = normWeight * depthWeight * gaussianWeight;
      weight += w;
      ss += t * w;
    }
  }
  
  fragData = vec4(ss / weight, 1.0);
  //fragData = texelFetch(m_AOTex, posSS, lod);
}
