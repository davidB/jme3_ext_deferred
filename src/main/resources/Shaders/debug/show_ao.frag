uniform sampler2D m_AOBuffer;

in vec2 texCoord;

out vec4 out_FragColor;

void main(){
    vec4 diffuseColor = texture(m_AOBuffer, texCoord);
    out_FragColor.rgb = vec3(diffuseColor.r);
}

