#import "ShaderLib/DeferredUtils.glsllib"

uniform sampler2D m_DepthBuffer;
uniform vec2 m_FrustumNearFar;

in vec2 texCoord;

out vec4 out_FragColor;

void main(){
	float depth = readDepth(m_DepthBuffer, texCoord, m_FrustumNearFar.x, m_FrustumNearFar.y);
	out_FragColor = vec4(vec3(depth), 1.0);
}

