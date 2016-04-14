Coordinates Systems
==================

* OS: Object Space (aka Model Space)
  * origin: origin of the object (anchor point in WS, root of bones)
  * unit: same as WS
  * type: vec3
* WS: World Space
  * origin: origin of the world (arbitrary)
  * unit: 3D space unit (eg meter, cm, arbitrary)
  * type: vec3
* ES: Eye Space (aka Camera Space or View Space)
  * don't use CS abbreviation to avoid confusion with Clip Space
  * don't use VS abbreviation to avoid visual confusion with WS
  * origin: eye, camera, light (anchor point in WS)
  * unit: same as WS
  * depth : raw, linear
  * z
  * near, far in same unit as WS
* PS: Projection Space (aka Clip Space)
  * don't use CS abbreviation to avoid confusion with Camera Space
  * orthographic, perspective
* NDC: Normalized Device Coordinates
  * range: ([-1.0, 1.0], [-1.0, 1.0], [-1.0, 1.0])
* SS: Screen Space (or Sample Space, Surface Space)
  * origin: bottom Left
  * name: type: unit 
    * uv/texCoord : vec2 ([0,1], [0,1])
    * posSS : vec2 ([0.5, width - 0.5], [0.5, height - 0.5]) (sub)texel
    * posSS : ivec2 ([0, width - 1], [0, height - 1]) pixel

## Functions for conversion

see ShaderLib/SpacesConverters.glsllib

## Links

* [What does gl_FragCoord contain? | txutxi.com](http://www.txutxi.com/?p=182)
* [OpenGL Transformation](http://www.songho.ca/opengl/gl_transform.html)
* [Coordinate Systems](http://learnopengl.com/#!Getting-started/Coordinate-Systems)
* [Coordinate Systems in OpenGL](http://www.matrix44.net/cms/notes/opengl-3d-graphics/coordinate-systems-in-opengl)
