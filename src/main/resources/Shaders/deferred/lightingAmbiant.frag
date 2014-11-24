uniform vec4 m_Color;
uniform sampler2D m_AOBuffer;
uniform sampler2D m_AlbedoBuffer;

in vec2 texCoord;

out vec4 out_FragColor;

void main(){
	vec4 albedo = texture2D(m_AlbedoBuffer, texCoord);
	float intensity = texture2D(m_AOBuffer, texCoord).r;
	out_FragColor = albedo * m_Color * intensity;
}
