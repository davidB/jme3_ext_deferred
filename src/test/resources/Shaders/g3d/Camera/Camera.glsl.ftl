// -*- c++ -*-
/** \file Camera/Camera.glsl

 G3D Innovation Engine (http://g3d.sf.net)
 Copyright 2000-2014, Morgan McGuire.
 All rights reserved.
*/
<#if !(.globals.Camera_glsl??)>
<#global Camera_glsl=1>
<#include "../compatibility.glsl.ftl">

/**
 \def uniform_Camera

 Declares frame (CameraToWorld matrix), previousFrame, clipInfo, and projInfo.
 On the host, invoke Camera::setShaderArgs
 to pass these values. Unused variables in the device
 shader will be removed by the compiler.

 \param name Include the underscore suffix, if a name is desired

 \sa G3D::Camera, G3D::Camera::setShaderArgs, G3D::Args, uniform_GBuffer
 */
<#macro uniform_Camera name>
    uniform mat4x3 ${name}frame;
    uniform mat4x3 ${name}previousFrame;
    uniform float3 ${name}clipInfo;
    uniform float4 ${name}projInfo;
</#macro>
</#if>