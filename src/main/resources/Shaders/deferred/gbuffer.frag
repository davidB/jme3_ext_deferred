#import "ShaderLib/DeferredUtils.glsllib"

uniform int m_MatId;

in vec3 vNormal;

out vec4 out_FragData[ 1 ];

void main(){
    vec3 normal = normalize(vNormal);

    out_FragData[0] = vec4(encodeNormal(normal), float(m_MatId) / 256.0);
}
