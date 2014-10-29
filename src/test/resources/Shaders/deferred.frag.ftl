#version 150 compatibility

<#include "g3d.ftl">
<#assign NUM_ENVIRONMENT_MAPS = 1>
<#assign NUM_LIGHTS = 2>
<#global G3D_SHADER_STAGE = G3D_FRAGMENT_SHADER>
<#include "g3d/deferred.pix.ftl">