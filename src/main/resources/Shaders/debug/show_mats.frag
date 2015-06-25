uniform sampler2D m_MatBuffer;

in vec2 texCoord;

out vec4 out_FragColor;

void main(){
    out_FragColor = texture(m_MatBuffer, texCoord);
}

