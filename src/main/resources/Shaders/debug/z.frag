out vec4 out_FragColor;

void main(){
  out_FragColor = vec4(vec3(step(0.99,gl_FragCoord.z)), 1.0);
  //out_FragColor = vec4(vec3(1.0 / gl_FragCoord.w), 1.0);
}
