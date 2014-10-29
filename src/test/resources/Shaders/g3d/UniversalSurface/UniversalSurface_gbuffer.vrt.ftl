//#version 120 or 420 compatibility// -*- c++ -*-
/**
 \file UniversalSurface_gbuffer.vrt
 \author Morgan McGuire
 \created 2007-10-22
 \edited  2013-06-11
 */
<#include "UniversalSurface_vertex.glsl.ftl">

<@expect name="USE_BONES" msg="1 or 0"/>

attribute vec4 g3d_Vertex;
attribute vec3 g3d_Normal;
attribute vec2 g3d_TexCoord0;
attribute vec4 g3d_PackedTangent;
attribute vec4 g3d_VertexColor;

#if USE_BONES
    attribute vec4      g3d_BoneWeights;
    attribute ivec4     g3d_BoneIndices;
    uniform sampler2D   boneMatrixTexture;
#   if defined(CS_POSITION_CHANGE) || defined(SS_POSITION_CHANGE) || defined(SS_EXPRESSIVE_MOTION)
        uniform sampler2D   prevBoneMatrixTexture;
#   endif
#endif



#if defined(NUM_LIGHTMAP_DIRECTIONS) && (NUM_LIGHTMAP_DIRECTIONS > 0)
    attribute vec2 g3d_TexCoord1;
#endif

#if defined(CS_POSITION_CHANGE) || defined(SS_POSITION_CHANGE)
    uniform mat4x3  PreviousObjectToCameraMatrix;
    varying layout(location=7) vec3 csPrevPosition;
#endif

#if defined(SS_EXPRESSIVE_MOTION)
    uniform mat4x3  ExpressivePreviousObjectToCameraMatrix;
    varying layout(location=8) vec3 csExpressivePrevPosition;
#endif

void main(void) {
    // Temporary variables needed because some drivers do not allow modifying attribute variables directly
    vec4 vertex         = g3d_Vertex;
    vec3 normal         = g3d_Normal;
    vec4 packedTangent  = g3d_PackedTangent;


#   if (defined(CS_POSITION_CHANGE) || defined(SS_POSITION_CHANGE) || defined(SS_EXPRESSIVE_MOTION)) && (USE_BONES == 1)
        // For expressive motion, still use the same previous bone matrix texture to avoid
        // additional computation (since it is expressive anyway!)
        mat4 prevBoneTransform = UniversalSurface_getFullBoneTransform(g3d_BoneWeights, g3d_BoneIndices, prevBoneMatrixTexture);
#   endif

#   if defined(CS_POSITION_CHANGE) || defined(SS_POSITION_CHANGE)
#       if USE_BONES == 1
            csPrevPosition = (PreviousObjectToCameraMatrix * (prevBoneTransform * vertex)).xyz;
#       else
            csPrevPosition = (PreviousObjectToCameraMatrix * vertex).xyz;
#       endif
#   endif

#   if defined(SS_EXPRESSIVE_MOTION)
#       if USE_BONES == 1
            csExpressivePrevPosition = (ExpressivePreviousObjectToCameraMatrix * (prevBoneTransform * vertex)).xyz;
#       else
            csExpressivePrevPosition = (ExpressivePreviousObjectToCameraMatrix * vertex).xyz;
#       endif
#   endif

#   if USE_BONES == 1
        // This mutates vertex, normal, and packedTangent
        UniversalSurface_boneTransform(g3d_BoneWeights, g3d_BoneIndices, boneMatrixTexture, vertex, normal, packedTangent);
#   endif

    UniversalSurface_transform(vertex, normal, packedTangent, g3d_TexCoord0,
#       if defined(NUM_LIGHTMAP_DIRECTIONS) && (NUM_LIGHTMAP_DIRECTIONS > 0)
            g3d_TexCoord1,
#       else
            vec2(0),
#       endif
#if HAS_VERTEX_COLORS
            g3d_VertexColor
#else
            vec4(0)
#endif
        );
}
