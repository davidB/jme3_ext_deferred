uniform sampler2D m_SpecularBuffer;

in vec2 texCoord;

out vec4 out_FragColor;

void main(){
    vec2 newTexCoord = texCoord;
    vec4 v = texture2D(m_SpecularBuffer, newTexCoord);
    out_FragColor.rgb = v.rgb;
}

