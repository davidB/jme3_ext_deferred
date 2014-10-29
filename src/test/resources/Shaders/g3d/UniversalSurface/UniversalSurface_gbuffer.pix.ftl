//#version 120 or 150 compatibility or 420 compatibility // -*- c++ -*-
/**
  \file UniversalSurface_GBuffer.pix
  \author Morgan McGuire, http://graphics.cs.williams.edu

  This shader expects a prefix (GBuffer::macros() or SVO::macros()) to be attached
  at runtime using Shader::setPremable.

  If USE_IMAGE_STORE is defined and set to a non-zero value, then the OpenGL image store
  API is used instead of framebuffer write.  In this case, you will probably want to
  disable framebuffer writes (color mask) using RenderDevice::setColorWrite(false).

  Requires BUFFER_WIDTH_MASK = width - 1 and BUFFER_WIDTH_SHIFT = log_2(width) to be
  passed, where width is a power of 2.
*/
#extension GL_ARB_shader_atomic_counters : enable
#extension GL_ARB_shader_image_load_store : enable
#extension GL_ARB_separate_shader_objects : enable

<#include "../compatibility.glsl.ftl">
<#include "../UniversalMaterial/UniversalMaterial.glsl.ftl">

<@expect name="USE_IMAGE_STORE" msg="1 or 0"/>
<@expect name="USE_DEPTH_PEEL" msg="1 or 0"/>
<@expect name="HAS_ALPHA" msg="1 or 0"/>
<@expect name="NUM_LIGHTMAP_DIRECTIONS" msg="0, 1, or 3"/>
#ifdef NORMALBUMPMAP
<@expect name="PARALLAXSTEPS" msg="int"/>
#endif

#if (__VERSION__ < 420)// && ! defined(GL_ARB_separate_shader_objects))
#   define layout(ignore)
#endif

<@uniform_UniversalMaterial name=""/>

/** Texture coordinate */
varying layout(location=0) vec2 texCoord;

varying layout(location=1) vec3 wsPosition;

#if USE_IMAGE_STORE
#   ifndef GL_ARB_shader_image_load_store
#       error "Using the SVO shaders requires the GL_ARB_shader_image_load_store extension"
#   endif

#   ifdef LAMBERTIAN
        uniform float4                          LAMBERTIAN_writeMultiplyFirst;
        uniform float4                          LAMBERTIAN_writeAddSecond;
        uniform writeonly image2D               LAMBERTIAN_buffer;

        // Declare the local variable (which is #defined to itself by SVO)
        float3                                  LAMBERTIAN;
#   endif

#   ifdef SVO_POSITION
        uniform float4                          SVO_POSITION_writeMultiplyFirst;
        uniform float4                          SVO_POSITION_writeAddSecond;
        uniform writeonly image2D               SVO_POSITION_buffer;
        float3                                  SVO_POSITION;
#   endif

#   ifdef WS_POSITION
        uniform float4                          WS_POSITION_writeMultiplyFirst;
        uniform float4                          WS_POSITION_writeAddSecond;
        uniform writeonly image2D               WS_POSITION_buffer;
        float3                                  WS_POSITION;
#   endif

#   if defined(GL_ARB_shader_atomic_counters) && 0
        // Image atomic backed by a PBO
        // Not currently implemented
        uniform atomic_uint                     fragmentCounter;
#   else
        // Fall back to image atomics, which do not require GL_ARB_shader_atomic_counters
        // but are not optimized for high numbers of collisions
        layout(r32ui) uniform  uimageBuffer     fragmentCounter_buffer;
#   endif
#else
    /** Do not read color attributes (except LAMBERTIAN, if an alpha test is required)
        outside of this rectangle.  Used to implement the trim band outside of which
        only depth is recorded. */
    uniform vec2            lowerCoord, upperCoord;
#endif

#if defined(CS_POSITION_CHANGE) || defined(SS_POSITION_CHANGE)
    varying layout(location=7) vec3 csPrevPosition;
#endif

#if defined(SS_EXPRESSIVE_MOTION)
    varying layout(location=8) vec3 csExpressivePrevPosition;
#endif

#if defined(SS_POSITION_CHANGE) || defined(SS_EXPRESSIVE_MOTION)
    // We reproject per-pixel so that csPrevPosition can be interpolated
    // linearly in the current frame's 3D; projecting the previous position
    // in the vertex shader would result in a previous homogeneous value
    // being linearly in the current time frame.
    uniform mat4 ProjectToScreenMatrix;
#endif

#if HAS_ALPHA
    // The alpha test value
    uniform float alphaThreshold;
#endif

float backside = (gl_FrontFacing == g3d_InvertY) ?  1.0 : -1.0;

#ifdef NORMALBUMPMAP
#   if (PARALLAXSTEPS > 0)
        /** Un-normalized (interpolated) tangent space eye vector */
        varying layout(location=6) vec3  _tsE;
#   endif
    varying layout(location=4)   vec3    tan_X;
    varying layout(location=5)   vec3    tan_Y;

#include <BumpMap/BumpMap.glsl>
#endif

varying layout(location=2) vec3             tan_Z;

/** Index of refraction / 24.0 */
uniform float            normalizedIndexOfRefraction;

#if HAS_VERTEX_COLORS
    varying layout(location=10) vec4 vertexColor;
#endif

#ifdef SVO_POSITION
    varying layout(location=8) vec3         svoPosition;
	flat varying layout(location=9) int		triangleAxis;
#endif

#if (USE_DEPTH_PEEL != 0)
#   include <depthPeel.glsl>
    /** Need not be at the same resolution as the current depth buffer.
        For samples to be written, they must be at greater gl_FragCoord.z values
        than those in this buffer.*/
    uniform sampler2D previousDepthBuffer;

    /**
     textureSize(previousDepthBuffer) / textureSize(currentDepthBuffer)
     */
    uniform float2    currentToPreviousScale;

    /** Minimum depth buffer value distance (on [0,1]) that new faces
        must be beyond previousDepthBuffer to be written. */
    uniform float     minZSeparation;

    uniform float3    clipInfo;
#endif

void main() {
#   if (USE_DEPTH_PEEL != 0)
        if (isDepthPeeled(previousDepthBuffer, currentToPreviousScale, minZSeparation, gl_FragCoord.xyz, clipInfo)) {
            // We have to discard here to avoid writing to z, even though it causes us to lose early z tests on 2013-era hardware
            discard;
        }
#   endif

#   if ! USE_IMAGE_STORE
       // Check the colorrect bounds
       if ((gl_FragCoord.x < lowerCoord.x) ||
           (gl_FragCoord.y < lowerCoord.y) ||
           (gl_FragCoord.x > upperCoord.x) ||
           (gl_FragCoord.y > upperCoord.y)) {
            // Outside of bounds. Perform a fast, non-parallax alpha test if required.

#           if HAS_ALPHA
                // Don't bother with parallax--we're in a guard band
                float alpha = texture2D(LAMBERTIAN_buffer, texCoord).a * LAMBERTIAN_readMultiplyFirst.a + LAMBERTIAN_readAddSecond.a;
#               if HAS_VERTEX_COLORS
                    alpha *= vertexColor.a;
#               endif
                if (alpha < alphaThreshold) {
                    // We have to discard because FBO might not be using the lambertian buffer in gl_FragCoord[0]
                    discard;
                }
#           endif

            // Don't bother looking up attributes, just let the depth write straight through
            return;
       }
#   endif

#   if defined(NORMALBUMPMAP)
        float rawNormalLength = 1.0;
        vec3 wsN;
        vec2 offsetTexCoord;
        vec3 tsN;
#       if (PARALLAXSTEPS > 0)
			// Requires bump map constants
            bumpMap(normalBumpMap, bumpMapScale, bumpMapBias, texCoord, tan_X, tan_Y, tan_Z, backside, normalize(_tsE), wsN, offsetTexCoord, tsN, rawNormalLength, PARALLAXSTEPS);
#       else
            bumpMap(normalBumpMap, 0.0, 0.0,                  texCoord, tan_X, tan_Y, tan_Z, backside, vec3(0.0),       wsN, offsetTexCoord, tsN, rawNormalLength, PARALLAXSTEPS);
#       endif
#   else
        // World space normal
        vec3 wsN = normalize(tan_Z.xyz * backside);
        vec2 offsetTexCoord = texCoord;
        // No bump maps, normal always Z-axis of tangent space
        vec3 tsN = vec3(0.0,0.0,1.0);
#   endif

    //////////////////////// MATERIAL //////////////////////////////

    vec3 lambertianColor;
    float coverage;
    {
        vec4 temp = texture2D(LAMBERTIAN_buffer, offsetTexCoord) * LAMBERTIAN_readMultiplyFirst + LAMBERTIAN_readAddSecond;
#       if HAS_VERTEX_COLORS
            temp *= vertexColor;
#       endif

        lambertianColor = temp.rgb;
        coverage = temp.a;
    }

#   if HAS_ALPHA
        if (coverage < alphaThreshold) {
            // We have to discard because FBO might not be using the lambertian buffer in gl_FragCoord[0]
            discard;
        }
#   endif
#   ifdef LAMBERTIAN
        LAMBERTIAN.rgb = lambertianColor;
#   endif

<#list [["EMISSIVE", "emissive", "3", "rgb"], ["TRANSMISSIVE", "transmissive", "3", "rgb"]] as i>
#       ifdef ${i[0]}
            ${i[0]}.${i[3]} =
#           if defined(${i[0]}CONSTANT) || defined(${i[0]}MAP)
#               ifdef ${i[0]}CONSTANT
                    ${i[1]}Constant
#                   ifdef ${i[0]}MAP
                        * texture2D(${i[1]}Map, offsetTexCoord).${i[3]})
#                   endif
#               else
                    texture2D(${i[1]}Map, offsetTexCoord).${i[3]}
#               endif
#           else
                vec${i[2]}(0.0)
#           endif
            ;
#       endif
</#list>

#   ifdef GLOSSY
        GLOSSY = texture2D(GLOSSY_buffer, offsetTexCoord) * GLOSSY_readMultiplyFirst + GLOSSY_readAddSecond;

#       if defined(NORMALBUMPMAP)
            // normal variance -> glossy coefficient to resolve aliasing
            if (GLOSSY.a < 1.0) {
                GLOSSY.a = packGlossyExponent(computeToksvigGlossyExponent(unpackGlossyExponent(GLOSSY.a), rawNormalLength));
            }
#       endif
#   endif

    ///////////////////////// NORMALS //////////////////////////////
#   ifdef CS_NORMAL
        vec3 csN = mat3(g3d_WorldToCameraMatrix) * wsN;
#   endif

#   if defined(WS_FACE_NORMAL) || defined(CS_FACE_NORMAL)
        vec3 wsFaceNormal = normalize(cross(dFdy(wsPosition), dFdx(wsPosition)));
#   endif

#   ifdef CS_FACE_NORMAL
        vec3 csFaceNormal = (g3d_WorldToCameraMatrix * vec4(wsFaceNormal, 0.0));
#   endif

<#list [["WS_NORMAL", "wsN"], ["CS_NORMAL", "csN"], ["TS_NORMAL", "tsN"], ["WS_FACE_NORMAL", "wsFaceNormal"], ["CS_FACE_NORMAL", "csFaceNormal"], ["SVO_POSITION", "svoPosition"]] as i>
#       ifdef ${i[0]}
            ${i[0]}.xyz = ${i[1]} * ${i[0]}_writeMultiplyFirst.xyz + ${i[0]}_writeAddSecond.xyz;
#       endif
</#list>

    //////////////////////// POSITIONS /////////////////////////////
    // NVIDIA drivers miscompile this unless we write WS_POSITION after the normals

#   if defined(CS_POSITION) || defined(CS_POSITION_CHANGE) || defined(SS_POSITION_CHANGE) || defined(SS_EXPRESSIVE_MOTION) || defined(CS_Z)
        vec3 csPosition = g3d_WorldToCameraMatrix * vec4(wsPosition, 1.0);
#   endif

#   ifdef CS_POSITION_CHANGE
        vec3 csPositionChange = csPosition - csPrevPosition;
#   endif

#   if defined(SS_POSITION_CHANGE) || defined(SS_EXPRESSIVE_MOTION)
        // gl_FragCoord.xy has already been rounded to a pixel center, so regenerate the true projected position.
        // This is needed to generate correct velocity vectors in the presence of Projection::pixelOffset
        vec4 accurateHomogeneousFragCoord = ProjectToScreenMatrix * vec4(csPosition, 1.0);
#   endif

#   ifdef SS_POSITION_CHANGE
        vec2 ssPositionChange;
        {
            if (csPrevPosition.z >= 0.0) {
                // Projects behind the camera; write zero velocity
                ssPositionChange = vec2(0.0);
            } else {
                vec4 temp = ProjectToScreenMatrix * vec4(csPrevPosition, 1.0);
                // We want the precision of division here and intentionally do not convert to multiplying by an inverse.
                // Expressing the two divisions as a single vector division operation seems to prevent the compiler from
                // computing them at different precisions, which gives non-zero velocity for static objects in some cases.
                // Note that this forces us to compute accurateHomogeneousFragCoord's projection twice, but we hope that
                // the optimizer will share that result without reducing precision.
                vec4 ssPositions = vec4(temp.xy, accurateHomogeneousFragCoord.xy) / vec4(temp.ww, accurateHomogeneousFragCoord.ww);

                ssPositionChange = ssPositions.zw - ssPositions.xy;
            }
        }
#   endif

#   ifdef SS_EXPRESSIVE_MOTION
        vec2 ssExpressiveMotion;
        {
            if (csExpressivePrevPosition.z >= 0.0) {
                // Projects behind the camera; write zero velocity
                ssExpressiveMotion = vec2(0.0);
            } else {
                vec4 temp = ProjectToScreenMatrix * vec4(csExpressivePrevPosition, 1.0);
                // We want the precision of division here and intentionally do not convert to multiplying by an inverse.
                // Expressing the two divisions as a single vector division operation seems to prevent the compiler from
                // computing them at different precisions, which gives non-zero velocity for static objects in some cases.
                // Note that this forces us to compute accurateHomogeneousFragCoord's projection twice, but we hope that
                // the optimizer will share that result without reducing precision.
                vec4 ssPositions = vec4(temp.xy, accurateHomogeneousFragCoord.xy) / vec4(vec2(temp.w), vec2(accurateHomogeneousFragCoord.w));

                ssExpressiveMotion = ssPositions.zw - ssPositions.xy;
            }
        }
#   endif


<#list [["WS_POSITION", "wsPosition", "xyz"], ["CS_POSITION", "csPosition", "xyz"], ["CS_POSITION_CHANGE", "csPositionChange", "xyz"], ["SS_POSITION_CHANGE", "ssPositionChange", "xy"], ["SS_EXPRESSIVE_MOTION", "ssExpressiveMotion", "xy"]] as i>
#       ifdef ${i[0]}
            ${i[0]}.${i[2]} = ${i[1]} * ${i[0]}_writeMultiplyFirst.${i[2]} + ${i[0]}_writeAddSecond.${i[2]};
#       endif
</#list>


#   ifdef CS_Z
        CS_Z.r = csPosition.z * CS_Z_writeMultiplyFirst.x + CS_Z_writeAddSecond.x;
#   endif

#   if USE_IMAGE_STORE
    {

#   if 1	//Standard path

        uint fragmentCount = uint(imageAtomicAdd(fragmentCounter_buffer, 0, uint(1)));
        int2 outCoord = int2(int(fragmentCount & uint(BUFFER_WIDTH_MASK)), int(fragmentCount >> uint(BUFFER_WIDTH_SHIFT)));


        // Write to the gbuffer using the image API
#       ifdef LAMBERTIAN
            imageStore(LAMBERTIAN_buffer, outCoord, float4(LAMBERTIAN, 0));
#       endif

#       ifdef WS_POSITION
            imageStore(WS_POSITION_buffer, outCoord, float4(WS_POSITION, 0));
#       endif

#       ifdef SVO_POSITION
            imageStore(SVO_POSITION_buffer, outCoord, clamp(float4(SVO_POSITION, 0), vec4(0), vec4(1)));
#       endif

        // TODO: Repeat for all fields
# else
		//Experimental path

		//float curResF=float(curRes);
		//float eyeZ=(inPos.z+1.0f)*0.5f *curResF;

        // Convert to depth buffer value factoring in gl_DepthRange
		float eyeZ = (2.0 * gl_FragCoord.z - gl_DepthRange.near - gl_DepthRange.far) / (gl_DepthRange.far - gl_DepthRange.near);

		vec2 eyeZDxDy= vec2(dFdx( eyeZ ), dFdy( eyeZ ))*0.5f;// *0.5f;

		vec4 eyeZCorners=vec4( eyeZ-eyeZDxDy.x-eyeZDxDy.y, eyeZ +eyeZDxDy.x-eyeZDxDy.y, eyeZ -eyeZDxDy.x+eyeZDxDy.y, eyeZ +eyeZDxDy.x+eyeZDxDy.y);

		vec2 eyeZMinMax;
		eyeZMinMax.x=min(min(min(eyeZCorners.x, eyeZCorners.y), eyeZCorners.z), eyeZCorners.w);
		eyeZMinMax.y=max(max(max(eyeZCorners.x, eyeZCorners.y), eyeZCorners.z), eyeZCorners.w);


		vec2 voxZMinMax = eyeZMinMax;
		vec2 voxZOffset =voxZMinMax - eyeZ;
		const float offsetOffset =1.0f / 2048.0f;//abs(voxZMinMax.y-voxZMinMax.x)/0.050f;

		//for(float offset=voxZMinMax.x; offset<=voxZMinMax.y; offset+=offsetOffset)
		for (int offset = -1; offset <= +1; ++offset) {

			vec3 voxCoordsLoc = SVO_POSITION;
			if (triangleAxis == 2) {
				voxCoordsLoc.x += float(offset)*offsetOffset;
			} else if(triangleAxis == 1) {
				voxCoordsLoc.y += float(offset)*offsetOffset;
			} else {
				voxCoordsLoc.z += float(offset)*offsetOffset;
			}


			uint fragmentCount = uint(imageAtomicAdd(fragmentCounter_buffer, 0, 1U ));
			int2 outCoord = int2(int(fragmentCount & uint(BUFFER_WIDTH_MASK)), int(fragmentCount >> uint(BUFFER_WIDTH_SHIFT)));

			// Write to the gbuffer using the image API
#       ifdef LAMBERTIAN
            imageStore(LAMBERTIAN_buffer, outCoord, float4(LAMBERTIAN, 0));
#       endif

#       ifdef WS_POSITION
            imageStore(WS_POSITION_buffer, outCoord, float4(WS_POSITION, 0));
#       endif

#       ifdef SVO_POSITION
            imageStore(SVO_POSITION_buffer, outCoord, float4(voxCoordsLoc, 0));
#       endif
		}
#endif

    }
#   endif

}
/* end of SS_GBuffer.pix */
