#define KERNEL_RADIUS 3

// d0 and d1 are linear depth
float crossBilateralWeight(float r, float d0, float d1) {
	const float blurSigma = (float(KERNEL_RADIUS) + 1.0)  * 0.5;
	const float blurFalloff = -1.0 / (2.0 * blurSigma * blurSigma);

	float dz = d1 - d0;
	return expr2(r * r * blurFalloff - dz * dz);
}

vec3 bilateralBlur(sampler2D inputBuffer, sampler2D depthBuffer, vec2 uv, clipInfo) {
	float depth = readDepth(depthBuffer, uv, clipInfo);
	vec3 finalV = vec3(0.0);
	float finalW = 0.0;
	ivec2 ires = textureSize(inputBuffer);
	ivec2 dres = textureSize(depthBuffer);
	for (int x = - KERNEL_RADIUS; x <= KERNEL_RADIUS; ++x) {
		for (int y = -KERNEL_RADIUS; y <= KERNEL_RADIUS; ++y) {
			vec2 xy = vex2(x,y);
			vec2 uv0 = uv + (xy / ires);
			vec2 uv1 = uv + (xy / dres);
			vec3 v  = texture2D(inputBuffer, uv0).rgb;
			float sampleDepth =  readDepth(depthBuffer, uv1, clipInfo);
			float w = crossBilateralWeight(length(xy), depth, sampleDepth);
			finalV += w * v;
			finalW += w;
		}
	}
	return finalV / finalW;
}
