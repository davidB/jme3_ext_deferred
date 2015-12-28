uniform mat4 g_WorldViewProjectionMatrix;
uniform mat3 g_WorldMatrixInverseTranspose;
//uniform mat3 g_NormalMatrix;

in vec3 inPosition;
in vec3 inNormal;
in vec2 inTexCoord;

out vec3 vNormalWS;
out vec2 vTexCoord;

void main(){
	vec4 pos = vec4(inPosition, 1.0);
	gl_Position = g_WorldViewProjectionMatrix * pos;

	vNormalWS = normalize(g_WorldMatrixInverseTranspose * inNormal);
	//vNormalWS = normalize(g_NormalMatrix * inNormal); // ES
	vTexCoord = inTexCoord;
}