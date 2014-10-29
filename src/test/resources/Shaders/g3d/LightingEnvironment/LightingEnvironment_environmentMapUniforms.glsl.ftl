// -*- c++ -*-
/** \file LightingEnvironment/LightingEnvironment_environmentMapUniforms.glsl */
<#if !(.globals.LightingEnvironment_environmentMapUniforms_glsl??)>
<#global LightingEnvironment_environmentMapUniforms_glsl=1>


//#extension GL_ARB_texture_query_lod : enable


<#include "../g3dmath.glsl.ftl">

<#if !(NUM_ENVIRONMENT_MAPS??)><#stop "undefined NUM_ENVIRONMENT_MAPS : Integer for number of environment maps to be blended"></#if>
<#if (NUM_ENVIRONMENT_MAPS > 0)>
<#assign envMapMax = NUM_ENVIRONMENT_MAPS -1/>
<#list 0..envMapMax as i>
    /** The cube map with default OpenGL MIP levels */
    uniform samplerCube environmentMap${i}_buffer;

    /** Includes the weight for interpolation factors, the environment map's native scaling,
        and a factor of PI */
    uniform vec4        environmentMap${i}_readMultiplyFirst;

    /** log2(environmentMap.width * sqrt(3)) */
    uniform float       environmentMap${i}_glossyMIPConstant;
</#list>


/** Uses the globals:
  NUM_ENVIRONMENT_MAPS
  environmentMap{i}_buffer
  environmentMap{i}_scale
*/
Color3 computeLambertianEnvironmentMapLighting(Vector3 wsN) {
    Color3 E_lambertianAmbient = Color3(0.0);

<#list 0..envMapMax as i>
    {
        // Sample the highest MIP-level to approximate Lambertian integration over the hemisphere
        const float MAXMIP = 20;
        E_lambertianAmbient +=
#           if defined(environmentMap${i}_notNull)
                textureCubeLod(environmentMap${i}_buffer, wsN, MAXMIP).rgb *
#           endif
            environmentMap${i}_readMultiplyFirst.rgb;
    }
</#list>

    return E_lambertianAmbient;
}


/** Uses the globals:
  NUM_ENVIRONMENT_MAPS
  environmentMap{i}_buffer
  environmentMap{i}_scale
*/
Color3 computeGlossyEnvironmentMapLighting(Vector3 wsR, bool isMirror, float glossyExponent) {

    Color3 E_glossyAmbient = Color3(0.0);

    // We compute MIP levels based on the glossy exponent for non-mirror surfaces
    float MIPshift = isMirror ? 0.0 : -0.5 * log2(glossyExponent + 1.0);
<#list 0..envMapMax as i>
    {
        float MIPlevel = isMirror ? 0.0 : (environmentMap${i}_glossyMIPConstant + MIPshift);
#       if (__VERSION__ >= 400) || defined(GL_ARB_texture_query_lod)
            MIPlevel = max(MIPlevel, textureQueryLod(environmentMap${i}_buffer, wsR).y);
#       endif
        E_glossyAmbient +=
#           if defined(environmentMap${i}_notNull)
                textureCubeLod(environmentMap${i}_buffer, wsR, MIPlevel).rgb *
#           endif
            environmentMap${i}_readMultiplyFirst.rgb;
    }
</#list>

    return E_glossyAmbient;
}
</#if>
</#if>
