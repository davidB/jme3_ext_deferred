// 2014 - [A Survey of Efficient Representations for Independent Unit Vectors](http://jcgt.org/published/0003/02/01/)
// 2010 - [Compact Normal Storage for Small G-Buffers](http://aras-p.info/texts/CompactNormalStorage.html)
// 2009 - [Encoding floats to RGBA - the final?](http://aras-p.info/blog/2009/07/30/encoding-floats-to-rgba-the-final/)

// return +/- 1
vec2 signNotZero(vec2 v) {
	return vec2((v.x >= 0.0) ? +1.0 : -1.0, (v.y >= 0.0) ? +1.0 : -1.0);
}

// Assume normalized input. Output is on [-1, 1] for each component.
vec2 float32x3_to_oct(in vec3 v) {
	// Project the sphere onto the octahedron, and then onto the xy plane
	vec2 p = v.xy * (1.0 / (abs(v.x) + abs(v.y) + abs(v.z)));
	// Reflect the folds of the lower hemisphere over the diagonals
	return (v.z <= 0.0) ? ((1.0 - abs(p.yx)) * signNotZero(p)) : p;
}

vec3 oct_to_float32x3(vec2 e) {
	vec3 v = vec3(e.xy, 1.0 - abs(e.x) - abs(e.y));
	if (v.z < 0) v.xy = (1.0 - abs(v.yx)) * signNotZero(v.xy);
	return normalize(v);
}

// Assume normalized input on +Z hemisphere.
// Output is on [-1, 1].
vec2 float32x3_to_hemioct(in vec3 v) {
	// Project the hemisphere onto the hemi-octahedron,
	// and then into the xy plane
	vec2 p = v.xy * (1.0 / (abs(v.x) + abs(v.y) + v.z));
	// Rotate and scale the center diamond to the unit square
	return vec2(p.x + p.y, p.x - p.y);
}

vec3 hemioct_to_float32x3(vec2 e) {
	// Rotate and scale the unit square back to the center diamond
	vec2 temp = vec2(e.x + e.y, e.x - e.y) * 0.5;
	vec3 v = vec3(temp, 1.0 - abs(temp.x) - abs(temp.y));
	return normalize(v);
}

/* The caller should store the return value into a GL_RGB8 texture or attribute without modification. */
vec3 snorm12x2_to_unorm8x3(vec2 f) {
	vec2 u = vec2(round(clamp(f, -1.0, 1.0) * 2047 + 2047));
	float t = floor(u.y / 256.0);
	// If storing to GL_RGB8UI, omit the final division
	return floor(vec3(u.x / 16.0, fract(u.x / 16.0) * 256.0 + t, u.y - t * 256.0)) / 255.0;
}

vec2 unorm8x3_to_snorm12x2(vec3 u) {
	u *= 255.0;
	u.y *= (1.0 / 16.0);
	vec2 s = vec2(u.x * 16.0 + floor(u.y), fract(u.y) * (16.0 * 256.0) + u.z);
	return clamp(s * (1.0 / 2047.0) - 1.0, vec2(-1.0), vec2(1.0));
}
