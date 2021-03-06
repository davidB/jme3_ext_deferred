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

in vec3 vNormal;
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
	#if defined(NORMALMAP) && !defined(VERTEX_LIGHTING)
		vec4 normalHeight = texture(m_NormalMap, texCoord);
		//Note the -2.0 and -1.0. We invert the green channel of the normal map,
		//as it's complient with normal maps generated with blender.
		//see http://hub.jmonkeyengine.org/forum/topic/parallax-mapping-fundamental-bug/#post-256898
		//for more explanation.
		vec3 normal = normalize((normalHeight.xyz * vec3(2.0,-2.0,2.0) - vec3(1.0,-1.0,1.0)));
		#ifdef LATC
			normal.z = sqrt(1.0 - (normal.x * normal.x) - (normal.y * normal.y));
		#endif
	#elif !defined(VERTEX_LIGHTING)
		vec3 normal = vNormal;
		#if !defined(LOW_QUALITY) && !defined(V_TANGENT)
			normal = normalize(normal);
		#endif
	#endif
    vec3 n = normalize(normal);
    out_FragData[0] = vec4(encodeNormal(normal), float(m_MatId) / 256.0);

    #ifdef COLORMAP
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
