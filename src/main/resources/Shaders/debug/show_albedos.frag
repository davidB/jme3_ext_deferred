uniform sampler2D m_AlbedoBuffer;

in vec2 texCoord;

out vec4 out_FragColor;

void main(){
    vec2 newTexCoord = texCoord;
    vec4 v = texture(m_AlbedoBuffer,  newTexCoord);
    out_FragColor.rgb = v.rgb;
}

