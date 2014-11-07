uniform vec4 m_Color;
uniform sampler2D m_AOBuffer;

in vec2 texCoord;

out vec4 out_FragColor;

void main(){
	float intensity = texture2D(m_AOBuffer, texCoord).r;
	out_FragColor = m_Color * intensity;
}
