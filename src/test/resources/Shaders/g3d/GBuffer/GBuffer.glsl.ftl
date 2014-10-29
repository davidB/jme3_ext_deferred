// -*- c++ -*-
/** \file GBuffer/GBuffer.glsl

 G3D Innovation Engine (http://g3d.sf.net)
 Copyright 2000-2014, Morgan McGuire.
 All rights reserved.
*/
<#if !(.globals.GBuffer_glsl??)>
<#global GBuffer_glsl=1>
<#include "../Texture/Texture.glsl.ftl">
<#include "../Camera/Camera.glsl.ftl">

/**
 \def uniform_GBuffer

 Declares all uniforms needed to read all fields of
 the GBuffer. On the host, invoke GBuffer::setShaderReadArgs
 to pass these values. Unused variables in the device
 shader will be removed by the compiler.

 \param name Include the underscore suffix, if a name is desired

 \sa G3D::GBuffer, G3D::GBuffer::setShaderArgsRead, G3D::Args, uniform_Texture
 */
<#macro uniform_GBuffer name>
    <@uniform_Texture dimension="2D" name="${name}LAMBERTIAN_"/>
    <@uniform_Texture dimension="2D" name="${name}GLOSSY_"/>
    <@uniform_Texture dimension="2D" name="${name}EMISSIVE_"/>
    <@uniform_Texture dimension="2D" name="${name}WS_NORMAL_"/>
    <@uniform_Texture dimension="2D" name="${name}CS_NORMAL_"/>
    <@uniform_Texture dimension="2D" name="${name}WS_FACE_NORMAL_"/>
    <@uniform_Texture dimension="2D" name="${name}CS_FACE_NORMAL_"/>
    <@uniform_Texture dimension="2D" name="${name}CS_POSITION_"/>
    <@uniform_Texture dimension="2D" name="${name}WS_POSITION_"/>
    <@uniform_Texture dimension="2D" name="${name}CS_POSITION_CHANGE_"/>
    <@uniform_Texture dimension="2D" name="${name}SS_POSITION_CHANGE_"/>
    <@uniform_Texture dimension="2D" name="${name}SS_EXPRESSIVE_MOTION_"/>
    <@uniform_Texture dimension="2D" name="${name}CS_Z_"/>
    <@uniform_Texture dimension="2D" name="${name}DEPTH_"/>
    <@uniform_Texture dimension="2D" name="${name}TS_NORMAL_"/>
    <@uniform_Texture dimension="2D" name="${name}SVO_POSITION_"/>
    <@uniform_Camera name="${name}camera_"/>
</#macro>
</#if>