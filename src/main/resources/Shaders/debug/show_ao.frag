uniform sampler2D m_AOBuffer;

in vec2 texCoord;

out vec4 out_FragColor;

void main(){
    vec4 diffuseColor = texture2D(m_AOBuffer, texCoord);
    out_FragColor.rgb = diffuseColor.rgb;
}

