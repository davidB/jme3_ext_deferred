//#version 120 or 420 compatibility// -*- c++ -*-
<#include "compatibility.glsl.ftl">
<#include "g3dmath.glsl.ftl">
<#include "LightingEnvironment/LightingEnvironment_uniforms.glsl.ftl">
<#include "Light/Light.glsl.ftl">
<#include "Texture/Texture.glsl.ftl">
<#include "GBuffer/GBuffer.glsl.ftl">
<#include "reconstructFromDepth.glsl.ftl">

// Declare a GBuffer with no prefix
<@uniform_GBuffer name=""/>

<@uniform_Texture dimension="2D" name="indirectRadiosity_"/>

#ifdef GL_EXT_gpu_shader4
    <@uniform_Texture dimension="2D" name="ambientOcclusion_"/>
    uniform ivec2       ambientOcclusion_offset;
#endif

uniform float   saturatedLightBoost;
uniform float   unsaturatedLightBoost;

<#include "colorBoost.glsl.ftl">

void main() {
    // Screen-space point being shaded
    ivec2 C = ivec2(gl_FragCoord.xy);

    // Surface normal
    vec3 csN = texelFetch(CS_NORMAL_buffer, C, 0).xyz;
    vec3 wsN;
    if (dot(csN, csN) < 0.01) {
        // This is a background pixel, not part of an object
        discard;
    } else {
        wsN = normalize(mat3x3(camera_frame) * (csN * CS_NORMAL_readMultiplyFirst.xyz + CS_NORMAL_readAddSecond.xyz));
    }

    // Point being shaded
    float csZ = reconstructCSZ(texelFetch(DEPTH_buffer, C, 0).r, camera_clipInfo);
    vec3 csPosition = reconstructCSPosition(gl_FragCoord.xy, csZ, camera_projInfo);
    vec3 wsPosition = (camera_frame * vec4(csPosition, 1.0)).xyz;
    //texelFetch(WS_POSITION_buffer, C, 0).xyz * WS_POSITION_readScaleBias.x + WS_POSITION_readScaleBias.y;

    // View vector
    vec3 w_o = normalize(camera_frame[3] - wsPosition);

    // Lambertian coefficient in BSDF
    vec3 lambertianColor = texelFetch(LAMBERTIAN_buffer, C, 0).rgb * invPi;

    // Glossy coefficient in BSDF (this code unpacks
    // G3D::UniversalBSDF's encoding)
    Color4  F0 = texelFetch(GLOSSY_buffer, C, 0);
    float glossyExponent = unpackGlossyExponent(F0.a);

    float cos_o = dot(wsN, w_o);
    Color3 F = computeF(F0.rgb, max(0.0, cos_o), F0.a);
    lambertianColor *= 1.0 - F;
    // G = F * (s_X + 8) / (8 pi)
    Color3 glossyColor = F * (glossyExponent * (1.0 / (8.0 * pi)) + invPi);

    // Incoming reflection vector
    Vector3 w_mi = normalize(wsN * (2.0 * cos_o) - w_o);

    Color3 E_lambertianAmbient = computeLambertianEnvironmentMapLighting(wsN);
    Color3 E_glossyAmbient     = computeGlossyEnvironmentMapLighting(w_mi, (F0.a == 1.0), glossyExponent)
        * (8.0 / (glossyExponent + 8.0));

    Color3 E_glossy            = Color3(0);
    Color3 E_lambertian        = Color3(0);
    computeDirectLighting(wsN, w_o, wsN, 1.0, wsPosition, glossyExponent, E_lambertian, E_glossy);

    float AO =
#       if defined(GL_EXT_gpu_shader4) && defined(ambientOcclusion_notNull)
            (0.95 * texelFetch(ambientOcclusion_buffer, min(ivec2(gl_FragCoord.xy) + ambientOcclusion_offset, ivec2(ambientOcclusion_size.xy) - ivec2(1, 1)), 0).r + 0.05);
#       else
            1.0;
#       endif

    vec3 emissiveColor = texelFetch(EMISSIVE_buffer, C, 0).rgb * EMISSIVE_readMultiplyFirst.rgb + EMISSIVE_readAddSecond.rgb;

    // How much ambient occlusion to apply to direct illumination (sort of approximates area lights,
    // more importantly: NPR term that adds local contrast)
    const float aoInfluenceOnDirectIllumination = 0.65;
    float directAO = lerp(1.0, AO, aoInfluenceOnDirectIllumination);

    vec4 indirect = texelFetch(indirectRadiosity_buffer, C, 0);

    const float radiosityContrastCenter = 0.35;
    float radiosityConfidence = saturate(((1.0 - indirect.a) - radiosityContrastCenter) * 2.0 + radiosityContrastCenter);
    vec3 E_lambertianIndirect = radiosityConfidence * indirect.rgb * colorBoost(indirect.rgb, unsaturatedLightBoost, saturatedLightBoost);

#if NO_LIGHTPROBE
    E_lambertianAmbient = vec3(0.0);
    E_glossyAmbient     = vec3(0.0);
#endif

#if USE_INDIRECT
    gl_FragColor.rgb =
            emissiveColor +
           + (E_lambertian * directAO + E_lambertianIndirect * AO   + E_lambertianAmbient * AO * lerp(1.0, 0.3, radiosityConfidence)) * lambertianColor
           + (E_glossy     * directAO                               + E_glossyAmbient     * AO) * glossyColor;

#else
        gl_FragColor.rgb =
            emissiveColor +
           + (E_lambertian * directAO                               + E_lambertianAmbient * AO) * lambertianColor
           + (E_glossy     * directAO                               + E_glossyAmbient     * AO) * glossyColor;
#endif
}
