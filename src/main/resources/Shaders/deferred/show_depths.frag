#import "ShaderLib/DeferredUtils.glsllib"

uniform sampler2D m_DepthBuffer;
uniform vec2 g_FrustumNearFar;

in vec2 texCoord;

out vec4 out_FragColor;

void main(){
	float depth = readDepth(m_DepthBuffer, texCoord, g_FrustumNearFar.x, g_FrustumNearFar.y);
	out_FragColor = vec4(depth, depth, depth, 1.0);
}

