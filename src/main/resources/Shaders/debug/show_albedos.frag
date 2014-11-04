uniform sampler2D m_DiffuseBuffer;

in vec2 texCoord;

out vec4 out_FragColor;

void main(){
    vec2 newTexCoord = texCoord;
    vec4 diffuseColor = texture2D(m_DiffuseBuffer,  newTexCoord);
    out_FragColor.rgb = diffuseColor.rgb;
}

