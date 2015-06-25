in vec2 vTexCoord;
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
            if (texture(m_ColorMap, vTexCoord).a <= m_AlphaDiscardThreshold){
                discard;
            }
       #else
            if (texture(m_DiffuseMap, vTexCoord).a <= m_AlphaDiscardThreshold){
                discard;
            }
       #endif
   #endif

   out_FragColor = vec4(1.0);
}