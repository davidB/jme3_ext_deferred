uniform sampler2D m_Texture;
in vec2 texCoord;
out vec4 out_FragColor;
void main() {
      out_FragColor = texture2D(m_Texture, texCoord);
}
