#import "ShaderLib/DeferredUtils.glsllib"

uniform sampler2D m_DepthBuffer;
uniform sampler2D m_NormalBuffer;
uniform float m_Bias;
uniform float m_Intensity;
uniform vec3 m_SampleRadius;
uniform vec4 m_ProjInfo;
uniform float m_ProjScale;

uniform vec2 g_FrustumNearFar;
uniform vec2 g_Resolution;
uniform mat4 g_ViewMatrix;
uniform float g_Time;
uniform mat4 g_ProjectionMatrix;


in vec2 texCoord;

//#extension GL_EXT_gpu_shader4 : require
//#extension GL_ARB_gpu_shader5 : enable

#define rsqrt inversesqrt

#define lerp mix

float saturate(float x) {
	return clamp(x, 0.0, 1.0);
}

float square(float x) {
	return x * x;
}

/**
 \file AmbientOcclusion_AO.pix
 \author Morgan McGuire and Michael Mara, NVIDIA Research

 Reference implementation of the Scalable Ambient Obscurance (AmbientOcclusion) screen-space ambient obscurance algorithm.

 The optimized algorithmic structure of AmbientOcclusion was published in McGuire, Mara, and Luebke, Scalable Ambient Obscurance,
 <i>HPG</i> 2012, and was developed at NVIDIA with support from Louis Bavoil.

 The mathematical ideas of AlchemyAO were first described in McGuire, Osman, Bukowski, and Hennessy, The
 Alchemy Screen-Space Ambient Obscurance Algorithm, <i>HPG</i> 2011 and were developed at
 Vicarious Visions.

 DX11 HLSL port by Leonardo Zide of Treyarch

 <hr>

  Open Source under the "BSD" license: http://www.opensource.org/licenses/bsd-license.php

  Copyright (c) 2011-2012, NVIDIA
  All rights reserved.

  Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

  Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
  Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

  */
//#expect NUM_SAMPLES "Integer number of samples to take at each pixels"
#define NUM_SAMPLES 9
//#expect NUM_SPIRAL_TURNS "Integer number of turns around the circle that the spiral pattern makes. The G3D::AmbientOcclusion class provides a discrepancy-minimizing value of NUM_SPIRAL_TURNS for eac value of NUM_SAMPLES."
#define NUM_SPIRAL_TURNS 7
//#expect DIFFERENT_DEPTH_RESOLUTIONS "1 if the peeled depth buffer is at a different resolution than the primary depth buffer"
#define DIFFERENT_DEPTH_RESOLUTIONS 0
//#expect USE_DEPTH_PEEL "1 to enable, 0 to disable"
#define USE_DEPTH_PEEL 0
//#expect CS_Z_PACKED_TOGETHER "1 to enable, 0 to disable"
#define CS_Z_PACKED_TOGETHER 0
//#expect TEMPORALLY_VARY_SAMPLES "1 to enable, 0 to disable"
#define TEMPORALLY_VARY_SAMPLES 0

// If using depth mip levels, the log of the maximum pixel offset before we need to switch to a lower
// miplevel to maintain reasonable spatial locality in the cache
// If this number is too small (< 3), too many taps will land in the same pixel, and we'll get bad variance that manifests as flashing.
// If it is too high (> 5), we'll get bad performance because we're not using the MIP levels effectively
#define LOG_MAX_OFFSET (3)

// This must be less than or equal to the MAX_MIP_LEVEL defined in SAmbientOcclusion.cpp
#define MAX_MIP_LEVEL (5)

/** Used for preventing AO computation on the sky (at infinite depth) and defining the CS Z to bilateral depth key scaling.
    This need not match the real far plane but should not be much more than it.*/
const float FAR_PLANE_Z = -250.0;

#define HIGH_QUALITY 1

//////////////////////////////////////////////////

/** The height in pixels of a 1m object if viewed from 1m away.
    You can compute it from your projection matrix.  The actual value is just
    a scale factor on radius; you can simply hardcode this to a constant (~500)
    and make your radius value unitless (...but resolution dependent.)  */
//uniform float           projScale;
#define projScale m_ProjScale

/** Negative, "linear" values in world-space units */
//uniform sampler2D       CS_Z_buffer;
#define CS_Z_buffer m_DepthBuffer

#if (USE_DEPTH_PEEL != 0) && (CS_Z_PACKED_TOGETHER == 0)
    /** Negative, "linear" values in world-space units */
    uniform sampler2D       peeled_CS_Z_buffer;

#   if (DIFFERENT_DEPTH_RESOLUTIONS != 0)
    uniform float           peeledToUnpeeledScale;
#   endif

#endif

//#define normal_notNull 1

/** World-space AO radius in scene units (r).  e.g., 1.0m */
//uniform float           radius;
#define radius m_SampleRadius.x
//const float radius = 1.0;
//uniform float           radius2;
#define radius2 m_SampleRadius.y
//const float radius2 = radius * radius;
//uniform float           invRadius2;
#define invRadius2 m_SampleRadius.z
//const float invRadius2 = 1.0 / radius2;

/** Bias to avoid AO in smooth corners, e.g., 0.01m */
//uniform float           bias;
#define bias m_Bias
//const float bias = 0.01;

/** intensity / radius^6 */
//uniform float           intensityDivR6;
//uniform float           intensity;

// Compatibility with future versions of GLSL: the shader still works if you change the
// version line at the top to something like #version 330 compatibility.
#if __VERSION__ == 120
#   define texelFetch   texelFetch2D
#   define textureSize  textureSize2D
#else
    out vec3            out_FragColor;
#endif
#define visibility      out_FragColor.r
#define bilateralKey    out_FragColor.g

/////////////////////////////////////////////////////////

float reconstructCSZ(float d) {
    return reconstructCSZ(d, g_FrustumNearFar.x, g_FrustumNearFar.y);
}

vec3 reconstructCSPosition(vec2 S, float z) {
	return reconstructCSPosition(S, z, m_ProjInfo);
}

//------------------------------------------------------


/** Returns a unit vector and a screen-space radius for the tap on a unit disk
    (the caller should scale by the actual disk radius) */
vec2 tapLocation(int sampleNumber, float spinAngle, out float ssR){
    // Radius relative to ssR
    float alpha = float(sampleNumber + 0.5) * (1.0 / NUM_SAMPLES);
    float angle = alpha * (NUM_SPIRAL_TURNS * 6.28) + spinAngle;

    ssR = alpha;
    return vec2(cos(angle), sin(angle));
}

/** Used for packing Z into the GB channels */
float CSZToKey(float z) {
    return clamp(z * (1.0 / FAR_PLANE_Z), 0.0, 1.0);
}



/** Read the camera-space position of the point at screen-space pixel ssP */
vec3 getPosition(ivec2 ssP, sampler2D cszBuffer) {
    vec3 P;
    //P.z = texelFetch(cszBuffer, ssP, 0).r;
	P.z = reconstructCSZ(texelFetch(cszBuffer, ssP, 0).r);
    // Offset to pixel center
    P = reconstructCSPosition(vec2(ssP) + vec2(0.5), P.z);
    return P;
}


/** Read the camera-space position of the point at screen-space pixel ssP + unitOffset * ssR.  Assumes length(unitOffset) == 1.
    Use cszBufferScale if reading from the peeled depth buffer, which has been scaled by (1 / invCszBufferScale) from the original */
vec3 getOffsetPosition(ivec2 ssC, vec2 unitOffset, float ssR, sampler2D cszBuffer, float invCszBufferScale) {
    // Derivation:
    //  mipLevel = floor(log(ssR / MAX_OFFSET));
// #   ifdef GL_EXT_gpu_shader5
//         int mipLevel = clamp(findMSB(int(ssR)) - LOG_MAX_OFFSET, 0, MAX_MIP_LEVEL);
// #   else
        int mipLevel = clamp(int(floor(log2(ssR))) - LOG_MAX_OFFSET, 0, MAX_MIP_LEVEL);
// #   endif

    ivec2 ssP = ivec2(ssR * unitOffset) + ssC;

    vec3 P;

    // We need to divide by 2^mipLevel to read the appropriately scaled coordinate from a MIP-map.
    // Manually clamp to the texture size because texelFetch bypasses the texture unit
    ivec2 mipP = clamp(ssP >> mipLevel, ivec2(0), textureSize(CS_Z_buffer, mipLevel) - ivec2(1));

    //P.z = texelFetch(cszBuffer, mipP, mipLevel).r;
    P.z = reconstructCSZ(texelFetch(cszBuffer, mipP, mipLevel).r);

    // Offset to pixel center
    P = reconstructCSPosition((vec2(ssP) + vec2(0.5)) * invCszBufferScale, P.z);

    return P;
}


/** Read the camera-space position of the points at screen-space pixel ssP + unitOffset * ssR in both channels of the packed csz buffer.  Assumes length(unitOffset) == 1. */
void getOffsetPositions(ivec2 ssC, vec2 unitOffset, float ssR, sampler2D cszBuffer, out vec3 P0, out vec3 P1) {
// #   ifdef GL_EXT_gpu_shader5
//         int mipLevel = clamp(findMSB(int(ssR)) - LOG_MAX_OFFSET, 0, MAX_MIP_LEVEL);
// #   else
        int mipLevel = clamp(int(floor(log2(ssR))) - LOG_MAX_OFFSET, 0, MAX_MIP_LEVEL);
// #   endif

    ivec2 ssP = ivec2(ssR * unitOffset) + ssC;

    // We need to divide by 2^mipLevel to read the appropriately scaled coordinate from a MIP-map.
    // Manually clamp to the texture size because texelFetch bypasses the texture unit
    ivec2 mipP = clamp(ssP >> mipLevel, ivec2(0), textureSize(CS_Z_buffer, mipLevel) - ivec2(1));

    //vec2 Zs = texelFetch(cszBuffer, mipP, mipLevel).rg;

	vec2 Zs = vec2(reconstructCSZ(texelFetch(cszBuffer, mipP, mipLevel).r));
    // Offset to pixel center
    P0 = reconstructCSPosition((vec2(ssP) + vec2(0.5)), Zs.x);
    P1 = reconstructCSPosition((vec2(ssP) + vec2(0.5)), Zs.y);
}


float fallOffFunction(float vv, float vn, float epsilon) {
    // A: From the HPG12 paper
    // Note large epsilon to avoid overdarkening within cracks
    //  Assumes the desired result is intensity/radius^6 in main()
    // return float(vv < radius2) * max((vn - bias) / (epsilon + vv), 0.0) * radius2 * 0.6;

    // B: Smoother transition to zero (lowers contrast, smoothing out corners). [Recommended]
#   if HIGH_QUALITY
        // Epsilon inside the sqrt for rsqrt operation
        float f = max(1.0 - vv * invRadius2, 0.0); return f * max((vn - bias) * rsqrt(epsilon + vv), 0.0);
#   else
        // Avoid the square root from above.
        //  Assumes the desired result is intensity/radius^6 in main()
        float f = max(radius2 - vv, 0.0); return f * f * f * max((vn - bias) / (epsilon + vv), 0.0);
#   endif

    // C: Medium contrast (which looks better at high radii), no division.  Note that the
    // contribution still falls off with radius^2, but we've adjusted the rate in a way that is
    // more computationally efficient and happens to be aesthetically pleasing.  Assumes
    // division by radius^6 in main()
    // return 4.0 * max(1.0 - vv * invRadius2, 0.0) * max(vn - bias, 0.0);

    // D: Low contrast, no division operation
    //return 2.0 * float(vv < radius * radius) * max(vn - bias, 0.0);
}


/** Compute the occlusion due to sample point \a Q about camera-space point \a C with unit normal \a n_C */
float aoValueFromPositionsAndNormal(vec3 C, vec3 n_C, vec3 Q) {
    vec3 v = Q - C;
    float vv = dot(v, v);
    float vn = dot(v, n_C);
    const float epsilon = 0.001;

    // Without the angular adjustment term, surfaces seen head on have less AO
    return fallOffFunction(vv, vn, epsilon) * lerp(1.0, max(0.0, 1.5 * n_C.z), 0.35);
}


/** Compute the occlusion due to sample with index \a i about the pixel at \a ssC that corresponds
    to camera-space point \a C with unit normal \a n_C, using maximum screen-space sampling radius \a ssDiskRadius

    Note that units of H() in the HPG12 paper are meters, not
    unitless.  The whole falloff/sampling function is therefore
    unitless.  In this implementation, we factor out (9 / radius).

    Four versions of the falloff function are implemented below

    When sampling from the peeled depth buffer, make sure ssDiskRadius has been premultiplied by cszBufferScale
*/
float sampleAO(in ivec2 ssC, in vec3 C, in vec3 n_C, in float ssDiskRadius, in int tapIndex, in float randomPatternRotationAngle, in sampler2D cszBuffer, in float invCszBufferScale) {
    // Offset on the unit disk, spun for this pixel
    float ssR;
    vec2 unitOffset = tapLocation(tapIndex, randomPatternRotationAngle, ssR);

    // Ensure that the taps are at least 1 pixel away
    ssR = max(0.75, ssR * ssDiskRadius);

#   if (CS_Z_PACKED_TOGETHER != 0)
        vec3 Q0, Q1;
        getOffsetPositions(ssC, unitOffset, ssR, cszBuffer, Q0, Q1);

        return max(aoValueFromPositionsAndNormal(C, n_C, Q0), aoValueFromPositionsAndNormal(C, n_C, Q1));
#   else
        // The occluding point in camera space
        vec3 Q = getOffsetPosition(ssC, unitOffset, ssR, cszBuffer, invCszBufferScale);

        return aoValueFromPositionsAndNormal(C, n_C, Q);
#   endif
}

const float MIN_RADIUS = 3.0; // pixels

void main() {
    // Pixel being shaded
    //ivec2 ssC = ivec2(gl_FragCoord.xy);
    ivec2 ssC = ivec2(texCoord * g_Resolution);

    // World space point being shaded
    vec3 C = getPosition(ssC, CS_Z_buffer);

    bilateralKey = CSZToKey(C.z);

#   ifdef normal_notNull
        //vec3 n_C = texelFetch(normal_buffer, ivec2(gl_FragCoord.xy), 0).xyz;
        //n_C = normalize(n_C * normal_readMultiplyFirst.xyz + normal_readAddSecond.xyz);
        vec3 n_C = normalize((g_ViewMatrix * vec4(readNormal(m_NormalBuffer, texCoord), 1.0)).xyz);
#   else
        // Reconstruct normals from positions.
        vec3 n_C = reconstructNonUnitCSFaceNormal(C);
        // Since n_C is computed from the cross product of camera-space edge vectors from points at adjacent pixels, its magnitude will be proportional to the square of distance from the camera
        if (dot(n_C, n_C) > (square(C.z * C.z * 0.00006))) { // if the threshold # is too big you will see black dots where we used a bad normal at edges, too small -> white
            // The normals from depth should be very small values before normalization,
            // except at depth discontinuities, where they will be large and lead
            // to 1-pixel false occlusions because they are not reliable
            visibility = 1.0;
            return;
        } else {
            n_C = normalize(n_C);
        }
#   endif

    // Hash function used in the HPG12 AlchemyAO paper
    float randomPatternRotationAngle = (((3 * ssC.x) ^ (ssC.y + ssC.x * ssC.y))
#if TEMPORALLY_VARY_SAMPLES
        + g_Time
#endif
        ) * 10;

    // Choose the screen-space sample radius
    // proportional to the projected area of the sphere
    float ssDiskRadius = -1.0 * projScale * radius / C.z;

    if (ssDiskRadius <= MIN_RADIUS) {
        // There is no way to compute AO at this radius
        visibility = 1.0;
       //return;
    }

#   if USE_DEPTH_PEEL == 1
#       if DIFFERENT_DEPTH_RESOLUTIONS == 1
            float unpeeledToPeeledScale = 1.0 / peeledToUnpeeledScale;
#       endif
#   endif

    float sum = 0.0;
    for (int i = 0; i < NUM_SAMPLES; ++i) {
        sum += sampleAO(ssC, C, n_C, ssDiskRadius, i, randomPatternRotationAngle, CS_Z_buffer, 1);
    }

#   if HIGH_QUALITY
        float A = pow(max(0.0, 1.0 - sqrt(sum * (3.0 / NUM_SAMPLES))), m_Intensity);
#   else
		float intensityDivR6 = m_Intensity / pow(radius,6);
        float A = max(0.0, 1.0 - sum * intensityDivR6 * (5.0 / NUM_SAMPLES));
        // Anti-tone map to reduce contrast and drag dark region farther
        // (x^0.2 + 1.2 * x^4)/2.2
        A = (pow(A, 0.2) + 1.2 * A*A*A*A) / 2.2;
#   endif

    // Visualize random spin distribution
    //A = mod(randomPatternRotationAngle / (2 * 3.141592653589), 1.0);

    // Fade in as the radius reaches 2 pixels
    visibility = lerp(1.0, A, saturate(ssDiskRadius - MIN_RADIUS));
}
