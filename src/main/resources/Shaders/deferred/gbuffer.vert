uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldMatrix;

attribute vec3 inPosition;
attribute vec3 inNormal;

out vec3 vNormal;

void main(){
	vec4 pos = vec4(inPosition, 1.0);
	gl_Position = g_WorldViewProjectionMatrix * pos;

	vec4 wvNormal;
	wvNormal = vec4(inNormal, 0.0);
	vNormal = normalize( (g_WorldMatrix * wvNormal).xyz );
}