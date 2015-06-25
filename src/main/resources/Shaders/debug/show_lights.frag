uniform sampler2D m_LBuffer;

in vec2 texCoord;

out vec4 out_FragColor;

void main(){
    vec4 diffuseColor = texture(m_LBuffer, texCoord);
    out_FragColor.rgb = diffuseColor.rgb;
}

