uniform mat4 g_WorldViewProjectionMatrix;

attribute vec3 inPosition;
attribute vec2 inTexCoord;

varying vec2 texCoord;

void main(){
   texCoord = inTexCoord;
#if FULLVIEW
   gl_Position = vec4(sign(inPosition.xy-vec2(0.5)), 0.0, 1.0);
   //gl_Position = vec4(inPosition.xy, 0.0, 1.0);
#else
   gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1.0);
#endif
}