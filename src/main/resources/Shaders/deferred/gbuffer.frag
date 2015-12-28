#import "ShaderLib/DeferredUtils.glsllib"
//#import "Common/ShaderLib/Parallax.glsllib"
//#import "Common/ShaderLib/Optics.glsllib"

uniform int m_MatId;
uniform float m_AlphaDiscardThreshold;
uniform sampler2D m_AlphaMap;
uniform sampler2D m_NormalMap;

uniform vec4 m_Color;
uniform sampler2D m_ColorMap;

uniform vec4 m_Specular;
uniform sampler2D m_SpecularMap;

in vec3 vNormalWS;
in vec2 vTexCoord;

out vec4 out_FragData[ 3 ];

void main(){
    vec2 texCoord = vTexCoord;

	#ifdef ALPHAMAP
		float alpha = texture(m_AlphaMap, texCoord).r;
		if(alpha < m_AlphaDiscardThreshold){
			discard;
		}
	#endif
    vec3 normal = normalize(vNormalWS);
    out_FragData[0] = vec4(encodeNormal(normal), float(m_MatId) / 256.0);

    #ifdef COLORMAP
      //vec4 albedo = vec4(texCoord, 0.0, 1.0);
      vec4 albedo = texture(m_ColorMap, texCoord);
    #else
      vec4 albedo = m_Color;
    #endif
    out_FragData[1] = albedo;

    #ifdef SPECULARMAP
      vec4 specularColor = texture(m_SpecularMap, texCoord);
    #else
      vec4 specularColor = m_Specular;
    #endif
    out_FragData[2] = specularColor;
}
