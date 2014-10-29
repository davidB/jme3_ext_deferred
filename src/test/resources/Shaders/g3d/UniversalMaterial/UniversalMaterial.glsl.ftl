// -*- c++ -*-
/** \file UniversalMaterial/UniversalMaterial.glsl

 G3D Innovation Engine (http://g3d.sf.net)
 Copyright 2000-2014, Morgan McGuire.
 All rights reserved.
*/
<#if !(.globals.UniversalMaterial_glsl??)>
<#global UniversalMaterial_glsl=1>

<#include "../compatibility.glsl.ftl">
<#include "../Texture/Texture.glsl.ftl">

/**
 \def uniform_UniversalMaterial

 Declares all material properties. Additional macros will also be bound
 by UniversalMaterial::setShaderArgs:

 - name##NUM_LIGHTMAP_DIRECTIONS
 - name##NORMALBUMPMAP
 - name##PARALLAXSTEPS

 \param name Include the underscore suffix, if a name is desired

 \sa G3D::UniversalMaterial, G3D::UniversalMaterial::setShaderArgs, G3D::Args, uniform_Texture
 \beta
 */
<#macro uniform_UniversalMaterial name>
    <@uniform_Texture dimension="2D" name="${name}LAMBERTIAN_"/>
    <@uniform_Texture dimension="2D" name="${name}GLOSSY_"/>
	uniform vec3		${name}emissiveConstant;
	uniform sampler2D	${name}emissiveMap;
	uniform vec3		${name}transmissiveConstant;
	uniform sampler2D	${name}transmissiveMap;
	uniform vec4		${name}customConstant;
	uniform sampler2D	${name}customMap;
	uniform float		${name}lightMapConstant;
	uniform sampler2D	${name}lightMap0;
	uniform sampler2D	${name}lightMap1;
	uniform sampler2D	${name}lightMap2;
    uniform sampler2D   ${name}normalBumpMap;
    uniform float       ${name}bumpMapScale;
	uniform float       ${name}bumpMapBias;
    <@uniform_Texture dimension="2D" name="${name}customMap_"/>
</#macro>
</#if>
