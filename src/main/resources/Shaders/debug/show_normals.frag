#import "ShaderLib/DeferredUtils.glsllib"

uniform sampler2D m_NormalBuffer;

in vec2 texCoord;

out vec4 out_FragColor;

void main(){
	out_FragColor.rgb = readNormal(m_NormalBuffer, texCoord);
	out_FragColor.a = 1.0;
}

