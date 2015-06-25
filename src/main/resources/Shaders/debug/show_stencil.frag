#import "ShaderLib/DeferredUtils.glsllib"

uniform sampler2D m_DepthBuffer;

in vec2 texCoord;

out vec4 out_FragColor;

void main(){
	vec4 v = texture(m_DepthBuffer, texCoord);
	out_FragColor = vec4(vec3(v.a*0.5), 1.0);
}

