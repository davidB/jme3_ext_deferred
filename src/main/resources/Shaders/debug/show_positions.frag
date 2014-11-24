#import "ShaderLib/DeferredUtils.glsllib"

uniform sampler2D m_DepthBuffer;
uniform vec4 m_ProjInfo;
uniform vec3 m_ClipInfo;
uniform mat4 m_ViewMatrixInverse;
uniform mat4 m_ViewProjectionMatrixInverse;
uniform vec2 m_Resolution;

in vec2 texCoord;

out vec4 out_FragColor;

vec3 getWSPosition(vec2 posSS) {
	float depth = readRawDepth(m_DepthBuffer, texCoord);
	//return reconstructWSPositionFromDepth(posSS + vec2(0.5), depth, m_ProjInfo, m_ClipInfo, m_ViewMatrixInverse);
	//return reconstructCSPositionFromDepth(posSS + vec2(0.5), depth, m_ProjInfo, m_ClipInfo);
	vec4 pos = vec4(texCoord, depth, 1.0) * 2.0 - 1.0;
	pos = m_ViewProjectionMatrixInverse * pos;
	return pos.xyz / pos.w;
}
void main(){
	//vec2 posSS = gl_FragCoord.xy;
	vec2 posSS = texCoord * m_Resolution;
	out_FragColor.rgb = getWSPosition(posSS);
	//out_FragColor.rgb = vec3(10,0,0);
	out_FragColor.a = 1.0;
}

