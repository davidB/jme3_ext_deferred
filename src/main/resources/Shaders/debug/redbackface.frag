out vec4 out_FragColor;

void main(){
  out_FragColor = (gl_FrontFacing)? vec4(0.0,1.0,0.0,1.0) : vec4(1.0,0.0,0.0,1.0);
}
