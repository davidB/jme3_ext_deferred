#import "ShaderLib/DeferredUtils.glsllib"

uniform sampler2D m_DepthBuffer;
uniform sampler2D m_NormalBuffer;
uniform sampler2D m_MatBuffer;
uniform sampler2D m_AlbedoBuffer;
uniform sampler2D m_AOBuffer;
uniform sampler2D m_LBuffer;

uniform vec2 g_Resolution;

in vec2 texCoord;

out vec4 out_FragColor;

void main(){
	//vec2 posSS = gl_FragCoord.xy;
	//vec2 texCoord = posSS / g_Resolution;
	vec2 posSS = texCoord * g_Resolution;
	vec3 norWS = readNormal(m_NormalBuffer, texCoord);
	int matId = int(texelFetch(m_NormalBuffer, ivec2(posSS), 0).a * 256.0);
	//vec4 diffuse = texelFetch(m_MatBuffer, ivec2(0, matId), 0);
	vec3 albedo = texture2D(m_AlbedoBuffer, texCoord).rgb;
	float intensity = texture2D(m_AOBuffer, texCoord).r;
	//vec4 specular = texelFetch(m_MatBuffer, ivec2(1, matId), 0);
	float shininess = 0.5;
	vec4 lights = texture2D(m_LBuffer, texCoord);
	vec3 ldiffuse = intensity * lights.rgb; // + ambient ?
	float lspec = step(0.03, length(ldiffuse)) * lights.a;

//	out_FragColor.rgb = vec3(0.0,1.0,0.0);
//	out_FragColor.rgb = vec3(intensity);
	out_FragColor.rgb = albedo * ldiffuse;// + albedo * lspec;
	out_FragColor.a = 1.0;
	gl_FragDepth = readRawDepth(m_DepthBuffer, posSS / g_Resolution);
}
