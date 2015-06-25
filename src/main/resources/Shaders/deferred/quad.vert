uniform mat4 g_WorldViewProjectionMatrix;

in vec3 inPosition;
in vec2 inTexCoord;

out vec2 texCoord;

void main(){
   texCoord = inTexCoord;
#ifdef FULLVIEW
   gl_Position = vec4(sign(inPosition.xy-vec2(0.5)), 0.0, 1.0);
   //gl_Position = vec4(inPosition.xy, 0.0, 1.0);
#else
   gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1.0);
#endif
}
