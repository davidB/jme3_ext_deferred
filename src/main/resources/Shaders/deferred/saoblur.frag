#import "ShaderLib/DeferredUtils.glsllib"

#extension GL_EXT_gpu_shader4 : require

uniform sampler2D m_NormalBuffer;
uniform mat4 g_ViewMatrix;
uniform vec2 g_Resolution;

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

/**
  \file AmbientOcclusion_blur.pix
  \author Morgan McGuire and Michael Mara, NVIDIA Research

  \brief 7-tap 1D cross-bilateral blur using a packed depth key

  DX11 HLSL port by Leonardo Zide, Treyarch

  Open m_Texture under the "BSD" license: http://www.openm_Texture.org/licenses/bsd-license.php

  Copyright (c) 2011-2012, NVIDIA
  All rights reserved.

  Redistribution and use in m_Texture and binary forms, with or without modification, are permitted provided that the following conditions are met:

  Redistributions of m_Texture code must retain the above copyright notice, this list of conditions and the following disclaimer.
  Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

//////////////////////////////////////////////////////////////////////////////////////////////
// Tunable Parameters:

#define NUM_KEY_COMPONENTS 1

// The other parameters in this section must be passed in as macro values

/** Increase to make depth edges crisper. Decrease to reduce flicker. */
#define EDGE_SHARPNESS     (1.0)

/** Step in 2-pixel intervals since we already blurred against neighbors in the
    first AO pass.  This constant can be increased while R decreases to improve
    performance at the expense of some dithering artifacts.

    Morgan found that a scale of 3 left a 1-pixel checkerboard grid that was
    unobjectionable after shading was applied but eliminated most temporal incoherence
    from using small numbers of sample taps.
    */
#define SCALE               (1)

/** Filter radius in pixels. This will be multiplied by SCALE. */
#define R                   (3)


//////////////////////////////////////////////////////////////////////////////////////////////

/** Type of data to read from m_Texture.  This macro allows
    the same blur shader to be used on different kinds of input data. */
#define VALUE_TYPE        float

/** Swizzle to use to extract the channels of m_Texture. This macro allows
    the same blur shader to be used on different kinds of input data. */
#define VALUE_COMPONENTS   r

#define VALUE_IS_KEY       0

/** Channel encoding the bilateral key value (which must not be the same as VALUE_COMPONENTS) */
#if NUM_KEY_COMPONENTS == 2
#   define KEY_COMPONENTS     gb
#else
#   define KEY_COMPONENTS     g
#endif

#if __VERSION__ >= 330
// Gaussian coefficients
const float gaussian[R + 1] =
//    float[](0.356642, 0.239400, 0.072410, 0.009869);
//    float[](0.398943, 0.241971, 0.053991, 0.004432, 0.000134);  // stddev = 1.0
    float[](0.153170, 0.144893, 0.122649, 0.092902, 0.062970);  // stddev = 2.0
//      float[](0.111220, 0.107798, 0.098151, 0.083953, 0.067458, 0.050920, 0.036108); // stddev = 3.0
#endif

uniform sampler2D   m_Texture;

/** (1, 0) or (0, 1)*/
uniform vec2       m_Axis;


#if __VERSION__ == 120
#   define          texelFetch texelFetch2D
#else
out vec3            gl_FragColor;
#endif

#define  result         gl_FragColor.VALUE_COMPONENTS
#define  keyPassThrough gl_FragColor.KEY_COMPONENTS

#if NUM_KEY_COMPONENTS == 2
    /** Returns a number on (0, 1) */
    float unpackKey(vec2 p) {
        return p.x * (256.0 / 257.0) + p.y * (1.0 / 257.0);
    }
#else
    /** Returns a number on (0, 1) */
    float unpackKey(float p) {
        return p;
    }
#endif

uniform vec4        m_ProjInfo;

vec3 positionFromKey(float key, ivec2 ssC, vec4 pInfo) {
    float z = key * FAR_PLANE_Z;
    vec3 C = reconstructCSPosition(vec2(ssC) + vec2(0.5), z, pInfo);
    return C;
}

#ifdef normal_notNull
    /** Same size as result buffer, do not offset by guard band when reading from it */
    uniform sampler2D       normal_buffer;
    uniform vec4            normal_readMultiplyFirst;
    uniform vec4            normal_readAddSecond;
#endif
//#define normal_notNull 1
float calculateBilateralWeight(float key, float tapKey, ivec2 tapLoc, vec3 n_C, vec3 C) {
    // range domain (the "bilateral" weight). As depth difference increases, decrease weight.
    float depthWeight = max(0.0, 1.0 - (EDGE_SHARPNESS * 2000.0) * abs(tapKey - key));
    float k_normal = 1.0;
    float k_plane = 1.0;

    // Prevents blending over creases.
    float normalWeight = 1.0;
    float planeWeight = 1.0;

#   ifdef normal_notNull
        //vec3 tapN_C = texelFetch(normal_buffer, tapLoc, 0).xyz;
        vec3 tapN_C = normalize((g_ViewMatrix * vec4(readNormal(m_NormalBuffer, tapLoc / g_Resolution), 1.0)).xyz);
        //tapN_C = normalize(tapN_C * normal_readMultiplyFirst.xyz + normal_readAddSecond.xyz);

        float normalError = 1.0 - dot(tapN_C, n_C) * k_normal;
        normalWeight = max((1.0 - EDGE_SHARPNESS * normalError), 0.00);

        float lowDistanceThreshold2 = 0.001;

        vec3 tapC = positionFromKey(tapKey, tapLoc, m_ProjInfo);

        // Change in position in camera space
        vec3 dq = C - tapC;

        // How far away is this point from the original sample
        // in camera space? (Max value is unbounded)
        float distance2 = dot(dq, dq);

        // How far off the expected plane (on the perpendicular) is this point?  Max value is unbounded.
        float planeError = max(abs(dot(dq, tapN_C)), abs(dot(dq, n_C)));

        planeWeight = (distance2 < lowDistanceThreshold2) ? 1.0 :
                         pow(max(0.0, 1.0 - EDGE_SHARPNESS * 2.0 * k_plane * planeError / sqrt(distance2)), 2.0);


#   endif

    //normalWeight = 1.0;
    //planeWeight = 1.0;

    return depthWeight * normalWeight * planeWeight;
}

//#define MDB_WEIGHTS 0
void main() {
	ivec2 axis = ivec2(m_Axis); // jme-3.0 doesn't support ivec2 as uniform

#   if __VERSION__ < 330
        float gaussian[R + 1];
//      if R == 0, we never call this shader
#       if R == 1 // TODO: Actually calculate gaussian weights... this is just Mike winging it here
            gaussian[0] = 0.5; gaussian[1] = 0.25;
#       elif R == 2 // TODO: Actually calculate gaussian weights... this is just Mike winging it here
            gaussian[0] = 0.153170; gaussian[1] = 0.144893; gaussian[2] = 0.122649;
#       elif R == 3 // TODO: We are losing some base weight here...
            gaussian[0] = 0.153170; gaussian[1] = 0.144893; gaussian[2] = 0.122649; gaussian[3] = 0.092902;  // stddev = 2.0
#       elif R == 4
            gaussian[0] = 0.153170; gaussian[1] = 0.144893; gaussian[2] = 0.122649; gaussian[3] = 0.092902; gaussian[4] = 0.062970;  // stddev = 2.0
#       elif R == 5 // TODO: We are losing some base weight here...
            gaussian[0] = 0.111220; gaussian[1] = 0.107798; gaussian[2] = 0.098151; gaussian[3] = 0.083953; gaussian[4] = 0.067458; gaussian[5] = 0.050920;
#       elif R == 6
            gaussian[0] = 0.111220; gaussian[1] = 0.107798; gaussian[2] = 0.098151; gaussian[3] = 0.083953; gaussian[4] = 0.067458; gaussian[5] = 0.050920; gaussian[6] = 0.036108;
#       endif
#   endif

    ivec2 ssC = ivec2(gl_FragCoord.xy);

    vec4 temp = texelFetch(m_Texture, ssC, 0);

    keyPassThrough = temp.KEY_COMPONENTS;
    float key = unpackKey(keyPassThrough);

    VALUE_TYPE sum = temp.VALUE_COMPONENTS;

    if (key == 1.0) {
        // Sky pixel (if you aren't using depth keying, disable this test)
        result = sum;
        return;
    }

    // Base weight for depth falloff.  Increase this for more blurriness,
    // decrease it for better edge discrimination
    float BASE = gaussian[0];
    float totalWeight = BASE;
    sum *= totalWeight;
    vec3 n_C;
#   ifdef normal_notNull
        //n_C = normalize(texelFetch(normal_buffer, ssC, 0).xyz * normal_readMultiplyFirst.xyz + normal_readAddSecond.xyz);
        n_C = normalize((g_ViewMatrix * vec4(readNormal(m_NormalBuffer, ssC / g_Resolution), 1.0)).xyz);
#   endif

    vec3 C = positionFromKey(key, ssC, m_ProjInfo);

# if MDB_WEIGHTS==0
    for (int r = -R; r <= R; ++r) {
        // We already handled the zero case above.  This loop should be unrolled and the static branch optimized out,
        // so the IF statement has no runtime cost
        if (r != 0) {
            ivec2 tapLoc = ssC + axis * (r * SCALE);
            temp = texelFetch(m_Texture, tapLoc, 0);
            float      tapKey = unpackKey(temp.KEY_COMPONENTS);
            VALUE_TYPE value  = temp.VALUE_COMPONENTS;

            // spatial domain: offset gaussian tap
            float weight = 0.3 + gaussian[abs(r)];

            float bilateralWeight = calculateBilateralWeight(key, tapKey, tapLoc, n_C, C);

            weight *= bilateralWeight;
            sum += value * weight;
            totalWeight += weight;
        }
    }
#else

    float lastBilateralWeight = 9999.0;
    for (int r = -1; r >= -R; --r) {
        ivec2 tapLoc = ssC + axis * (r * SCALE);
        temp = texelFetch(m_Texture, tapLoc, 0);
        float      tapKey = unpackKey(temp.KEY_COMPONENTS);
        VALUE_TYPE value  = temp.VALUE_COMPONENTS;

        // spatial domain: offset gaussian tap
        float weight = 0.3 + gaussian[abs(r)];

        // range domain (the "bilateral" weight). As depth difference increases, decrease weight.
        float bilateralWeight = calculateBilateralWeight(key, tapKey, tapLoc, n_C, C);
        bilateralWeight = min(lastBilateralWeight, bilateralWeight);
        lastBilateralWeight = bilateralWeight;
        weight *= bilateralWeight;
        sum += value * weight;
        totalWeight += weight;
    }

    lastBilateralWeight = 9999.0;
    for (int r = 1; r <= R; ++r) {
        ivec2 tapLoc = ssC + axis * (r * SCALE);
        temp = texelFetch(m_Texture, tapLoc, 0);
        float      tapKey = unpackKey(temp.KEY_COMPONENTS);
        VALUE_TYPE value  = temp.VALUE_COMPONENTS;

        // spatial domain: offset gaussian tap
        float weight = 0.3 + gaussian[abs(r)];

        // range domain (the "bilateral" weight). As depth difference increases, decrease weight.
        float bilateralWeight = calculateBilateralWeight(key, tapKey, tapLoc, n_C, C);
        bilateralWeight = min(lastBilateralWeight, bilateralWeight);
        lastBilateralWeight = bilateralWeight;
        weight *= bilateralWeight;
        sum += value * weight;
        totalWeight += weight;
    }
#endif

    const float epsilon = 0.0001;
    result = sum / (totalWeight + epsilon);
}
