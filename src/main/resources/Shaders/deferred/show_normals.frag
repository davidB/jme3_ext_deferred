#import "ShaderLib/DeferredUtils.glsllib"

in vec2 texCoord;

uniform sampler2D m_NormalBuffer;

out vec4 out_FragColor;

void main(){
	out_FragColor.rgb = readNormal(m_NormalBuffer, texCoord);
}

