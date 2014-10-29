<#macro expect name msg>
#ifndef ${name}
#  error "expect ${name} : ${msg}"
#endif
</#macro>
<#global G3D_SHADER_STAGE = 0/>
<#global G3D_VERTEX_SHADER = 1/>
<#global G3D_FRAGMENT_SHADER = 2/>

uniform bool g3d_InvertY;

uniform mat4 g_ViewMatrixInverse;
#define g3d_CameraToWorldMatrix g_ViewMatrixInverse

uniform mat4 g_WorldMatrix;
#define g3d_ObjectToWorldMatrix g_WorldMatrix

uniform mat3 g_WorldMatrixInverseTranspose;
#define g3d_ObjectToWorldNormalMatrix g_WorldMatrixInverseTranspose

uniform mat4 g_WorldViewMatrix;
#define gl_ModelViewProjectionMatrixTranspose transpose(g_WorldViewProjectionMatrix)

uniform mat4 g_WorldViewProjectionMatrix;
#define gl_ModelViewMatrixTranspose transpose(g_WorldViewMatrix)

//need to glEnable(GL_CLIP_DISTANCE0 + i)
#define gl_ClipVertex gl_ClipDistance[0]

#extension GL_ARB_shader_atomic_counters : require