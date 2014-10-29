uniform sampler2D m_NormalBuffer;

in vec2 texCoord;

out vec4 out_FragColor;

void main(){
    vec2 newTexCoord = texCoord;
    int matId = int(texture2D(m_NormalBuffer,  newTexCoord).a * 256.0);
    out_FragColor.rgb = vec3(float(matId) / 256.0);
    out_FragColor.a = 1.0;
}

