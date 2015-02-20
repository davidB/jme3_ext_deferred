in vec2 texCoord;
out vec4 out_FragColor;

#ifdef DISCARD_ALPHA
   #ifdef COLOR_MAP
      uniform sampler2D m_ColorMap;
   #else
      uniform sampler2D m_DiffuseMap;
   #endif
    uniform float m_AlphaDiscardThreshold;
#endif


void main(){
   #ifdef DISCARD_ALPHA
       #ifdef COLOR_MAP
            if (texture2D(m_ColorMap, texCoord).a <= m_AlphaDiscardThreshold){
                discard;
            }
       #else
            if (texture2D(m_DiffuseMap, texCoord).a <= m_AlphaDiscardThreshold){
                discard;
            }
       #endif
   #endif

   out_FragColor = vec4(1.0);
}