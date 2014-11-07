uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldMatrix;

in vec3 inPosition;
in vec3 inNormal;
in vec2 inTexCoord;

out vec3 vNormal;
out vec2 vTexCoord;

void main(){
	vec4 pos = vec4(inPosition, 1.0);
	gl_Position = g_WorldViewProjectionMatrix * pos;

	vec4 wvNormal;
	wvNormal = vec4(inNormal, 0.0);
	vNormal = normalize( (g_WorldMatrix * wvNormal).xyz );

	vTexCoord = inTexCoord;
}