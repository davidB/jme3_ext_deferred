uniform sampler2D m_Texture;
in vec2 texCoord;
out vec4 out_FragColor;
void main() {
  vec4 c = texture(m_Texture, texCoord);
#ifdef R_ONLY
  out_FragColor = vec4(c.r, 0.0, 0.0, 1.0);
#elif defined G_ONLY
  out_FragColor = vec4(0.0, c.g, 0.0, 1.0);
#elif defined B_ONLY
  out_FragColor = vec4(0.0, 0.0, c.b, 1.0);
#elif defined A_ONLY
  out_FragColor = vec4(c.a, c.a, c.a, 1.0);
#elif defined RGB_ONLY
    out_FragColor = vec4(c.r, c.g, c.b, 1.0);
#elif defined UV_ONLY
  out_FragColor = vec4(texCoord.x, texCoord.y, 0.0, 1.0);
#else  
  out_FragColor = c;
#endif
}
