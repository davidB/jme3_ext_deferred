uniform mat4 g_WorldViewProjectionMatrix;

in vec3 inPosition;

void main(){
	vec4 pos = vec4(inPosition, 1.0);
	gl_Position = g_WorldViewProjectionMatrix * pos;
}