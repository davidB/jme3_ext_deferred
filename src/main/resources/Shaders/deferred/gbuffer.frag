#import "ShaderLib/DeferredUtils.glsllib"

in vec3 vNormal;

out vec4 out_FragData[ 3 ];

void main(){
    vec3 normal = normalize(vNormal);
    vec4 diffuseColor = vec4(1.0, 0.0, 0.0, 1.0);
    vec4 specularColor = vec4(1.0);

    out_FragData[0] = vec4(diffuseColor.rgb, 1.0);
    out_FragData[1] = vec4(encodeNormal(normal), 0.0);
    out_FragData[2] = vec4(specularColor.rgb, 1.0 / 128.0);
}
