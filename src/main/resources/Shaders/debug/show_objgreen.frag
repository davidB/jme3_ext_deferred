in vec2 texCoord;

uniform sampler2D m_DiffuseBuffer;

out vec4 out_FragColor;

void main(){
    vec2 newTexCoord = texCoord;
    vec4 diffuseColor = texture2D(m_DiffuseBuffer,  newTexCoord);
    if (diffuseColor.a == 0.0)
        discard;
    out_FragColor.rgb = vec3(0.0, 1.0, 0.0);
}

