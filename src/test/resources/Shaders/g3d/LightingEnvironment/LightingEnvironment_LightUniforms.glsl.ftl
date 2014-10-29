// -*- c++ -*-
/** \file LightingEnvironment/LightingEnvironment_LightUniforms.glsl */

<#if !(.globals.LightingEnvironment_LightUniforms_glsl??)>
<#global LightingEnvironment_LightUniforms_glsl=1>

<#include "../compatibility.glsl.ftl">
<#include "../g3dmath.glsl.ftl">
<#include "../Light/Light.glsl.ftl">
<#include "../Texture/Texture.glsl.ftl">

<#if !(NUM_LIGHTS??)><#stop "undefined NUM_LIGHTS : Integer number of direct light sources (and shadow maps)"></#if>
<#if (NUM_LIGHTS > 0)>
<#assign lightMax = NUM_LIGHTS -1/>
<#list 0..lightMax as i>
    /** World space light position */
    uniform vec4        light${i}_position;

    /** Power of the light */
    uniform vec3        light${i}_color;

    /** Spot light facing direction (unit length) */
    uniform vec3        light${i}_direction;

    /** w element is the spotlight cutoff angle.*/
    uniform vec4        light${i}_attenuation;

    /** Is this spotlight's field of view rectangular (instead of round)? */
    uniform bool        light${i}_rectangular;

    uniform vec3        light${i}_up;

    uniform vec3        light${i}_right;

    /** Radius of the light bulb itself; no relation to the light's effect sphere */
    uniform float       light${i}_radius;

#   if defined(light${i}_shadowMap_notNull)
        /** Modelview projection matrix used for the light's shadow map */
        uniform mat4                light${i}_shadowMap_MVP;
        uniform float               light${i}_shadowMap_bias;

        uniform_Texture(2DShadow,   light${i}_shadowMap_);
#   endif
</#list>


/**
 Uses global variables:

  light{i}_position
  light{i}_attenuation
  light{i}_direction
  light{i}_up
  light{i}_right
  light{i}_rectangular
  light{i}_radius
  light{i}_color
  light{i}_shadowMap_notNull
  light{i}_shadowMap_invSize
  light{i}_shadowMap_buffer
 */
void computeDirectLighting(Vector3 n, Vector3 w_o, Vector3 n_face, float backside, Point3 wsPosition, float glossyExponent, inout Color3 E_lambertian, inout Color3 E_glossy) {
    vec3 w_i;
<#list 0..lightMax as i>
    {
#       if defined(light${i}_shadowMap_notNull)
            // "Normal offset shadow mapping" http://www.dissidentlogic.com/images/NormalOffsetShadows/GDC_Poster_NormalOffset.png
            // Note that the normal bias must be > shadowMapBias${i} to prevent self-shadowing; we use 3x here so that most
            // glancing angles are ok.
            vec4 shadowCoord = light${i}_shadowMap_MVP * vec4(wsPosition + w_o * (1.5 * light${i}_shadowMap_bias) + n_face * (backside * 0.5 * light${i}_shadowMap_bias), 1.0);
            addShadowedLightContribution(n, w_o, wsPosition, glossyExponent,
                light${i}_position, light${i}_attenuation, light${i}_direction, light${i}_up, light${i}_right, light${i}_rectangular, light${i}_radius, light${i}_color,
                shadowCoord, light${i}_shadowMap_buffer, light${i}_shadowMap_invSize.xy,
                n_face, backside,
                E_lambertian, E_glossy, w_i);
#       else
            addLightContribution(n, w_o, wsPosition, glossyExponent,
                light${i}_position, light${i}_attenuation, light${i}_direction, light${i}_up, light${i}_right, light${i}_rectangular, light${i}_radius, light${i}_color,
                n_face, backside,
                E_lambertian, E_glossy, w_i);
#      endif
    }
</#list>
}
</#if>
</#if>
