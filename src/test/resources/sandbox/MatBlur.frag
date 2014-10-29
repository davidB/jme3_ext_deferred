#import "ShaderLib/DeferredUtils.glsllib"

uniform sampler2D m_Texture;
uniform sampler2D m_DepthTexture;
uniform vec2 g_Resolution;
uniform vec2 m_Scale;
uniform vec2 g_FrustumNearFar;

in vec2 texCoord;

out vec4 out_FragColor;

const float epsilon = 0.005;

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

    const int kernelSize=7;

    vec4 bilateralFilter(in sampler2D srcBuffer, in sampler2D depthBuffer, in vec2 uv, in float near, in float far) {
        vec4 color = vec4(0.0);

        vec2 sampleD;
        vec2 sampleT;
        float sum = 0.0;
        float coefZ;
        float Zp = readDepth(depthBuffer, uv, near, far);
	ivec2 resT = textureSize(srcBuffer);
	ivec2 resD = textureSize(depthBuffer);
        for(int i = -(kernelSize-1); i <= (kernelSize-1); i+=2) {
            for(int j = -(kernelSize-1); j <= (kernelSize-1); j+=2) {
                sampleD = texCoord + vec2(i,j) / resD;
                sampleT = texCoord + vec2(i,j) / resT;
                float zTmp = readDepth(depthBuffer, sampleD, near, far);
                coefZ = 1.0 / (epsilon + abs(Zp - zTmp));
                sum += coefZ;

                color += coefZ * texture2D(srcBuffer, sampleT);

            }
        }

        return color / sum;
    }


    vec4 convolutionFilter(in sampler2D srcBuffer, in sampler2D depthBuffer, in vec2 uv, in float near, in float far, in vec2 scale){
           vec4 sum = vec4(0.0);
            float x = uv.x;
            float y = uv.y;

            float zsum = 1.0;
        float Zp =readDepth(depthBuffer, uv, near, far);


        vec2 sample = vec2(x - 2.0 * scale.x, y - 2.0 * scale.y);
        float zTmp =readDepth(depthBuffer, sample, near, far);
        float coefZ = 1.0 / (epsilon + abs(Zp - zTmp));
        zsum += coefZ;
        sum += coefZ* texture2D( srcBuffer, sample);

        sample = vec2(x - 0.0 * scale.x, y - 2.0 * scale.y);
        zTmp =readDepth(depthBuffer, sample, near, far);
        coefZ = 1.0 / (epsilon + abs(Zp - zTmp));
        zsum += coefZ;
        sum += coefZ* texture2D( srcBuffer, sample);

        sample = vec2(x + 2.0 * scale.x, y - 2.0 * scale.y);
        zTmp =readDepth(depthBuffer, sample, near, far);
        coefZ = 1.0 / (epsilon + abs(Zp - zTmp));
        zsum += coefZ;
        sum += coefZ* texture2D( srcBuffer, sample);

        sample = vec2(x - 1.0 * scale.x, y - 1.0 * scale.y);
        zTmp =readDepth(depthBuffer, sample, near, far);
        coefZ = 1.0 / (epsilon + abs(Zp - zTmp));
        zsum += coefZ;
        sum += coefZ* texture2D( srcBuffer, sample);

        sample = vec2(x + 1.0 * scale.x, y - 1.0 * scale.y);
        zTmp =readDepth(depthBuffer, sample, near, far);
        coefZ = 1.0 / (epsilon + abs(Zp - zTmp));
        zsum += coefZ;
        sum += coefZ* texture2D( srcBuffer, sample);

        sample = vec2(x - 2.0 * scale.x, y - 0.0 * scale.y);
        zTmp =readDepth(depthBuffer, sample, near, far);
        coefZ = 1.0 / (epsilon + abs(Zp - zTmp));
        zsum += coefZ;
        sum += coefZ* texture2D( srcBuffer, sample);

        sample = vec2(x + 2.0 * scale.x, y - 0.0 * scale.y);
        zTmp =readDepth(depthBuffer, sample, near, far);
        coefZ = 1.0 / (epsilon + abs(Zp - zTmp));
        zsum += coefZ;
        sum += coefZ* texture2D( srcBuffer, sample);

        sample = vec2(x - 1.0 * scale.x, y + 1.0 * scale.y);
        zTmp =readDepth(depthBuffer, sample, near, far);
        coefZ = 1.0 / (epsilon + abs(Zp - zTmp));
        zsum += coefZ;
        sum += coefZ* texture2D( srcBuffer, sample);

        sample = vec2(x + 1.0 * scale.x, y + 1.0 * scale.y);
        zTmp =readDepth(depthBuffer, sample, near, far);
        coefZ = 1.0 / (epsilon + abs(Zp - zTmp));
        zsum += coefZ;
        sum += coefZ* texture2D( srcBuffer, sample);

        sample = vec2(x - 2.0 * scale.x, y + 2.0 * scale.y);
        zTmp =readDepth(depthBuffer, sample, near, far);
        coefZ = 1.0 / (epsilon + abs(Zp - zTmp));
        zsum += coefZ;
        sum += coefZ* texture2D( srcBuffer, sample);

        sample = vec2(x - 0.0 * scale.x, y + 2.0 * scale.y);
        zTmp =readDepth(depthBuffer, sample, near, far);
        coefZ = 1.0 / (epsilon + abs(Zp - zTmp));
        zsum += coefZ;
        sum += coefZ* texture2D( srcBuffer, sample);

        sample = vec2(x + 2.0 * scale.x, y + 2.0 * scale.y);
        zTmp =readDepth(depthBuffer, sample, near, far);
        coefZ = 1.0 / (epsilon + abs(Zp - zTmp));
        zsum += coefZ;
        sum += coefZ* texture2D( srcBuffer, sample);


        return  sum / zsum;
    }


    void main(){
      //out_FragColor = convolutionFilter(m_Texture, m_DepthTexture, texCoord, g_FrustumNearFar.x, g_FrustumNearFar.y, m_Scale);
      out_FragColor = bilateralFilter(m_Texture, m_DepthTexture, texCoord, g_FrustumNearFar.x, g_FrustumNearFar.y);
    }