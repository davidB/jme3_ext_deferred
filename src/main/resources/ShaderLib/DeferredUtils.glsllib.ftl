<#include "UnitVector.glsllib.ftl">

vec3 encodeNormal(in vec3 normal){
 return snorm12x2_to_unorm8x3(float32x3_to_oct(normal));
}

vec3 decodeNormal(in vec3 unorm8x3Normal){
 return oct_to_float32x3(unorm8x3_to_snorm12x2(unorm8x3Normal));
 //return hemioct_to_float32x3(unorm8x3_to_snorm12x2(intNormal.rgb));
}

vec3 readNormal(in sampler2D normalBuffer, in vec2 uv) {
	vec4 intNormal = texture2D(normalBuffer, uv);
	return decodeNormal(intNormal.rgb);
}

float readRawDepth(in sampler2D depthBuffer, in vec2 uv) {
    return texture2D(depthBuffer, uv).r;
}

//see http://www.geeks3d.com/20091216/geexlab-how-to-visualize-the-depth-buffer-in-glsl/
float readDepth(in sampler2D depthBuffer, in vec2 uv, float near, float far) {
    //float z = fetchTextureSample(depthBuffer, uv, 0).r;
    float z = readRawDepth(depthBuffer, uv);
    return (2.0 * near) / (far + near - z * (far - near));
    // from g3d/reconstructFromDepth/reconstructCSZ
    //return (far * near) / (z * (near - far) + far);
}

