[![CC-0](http://i.creativecommons.org/p/zero/1.0/88x31.png)](http://creativecommons.org/publicdomain/zero/1.0/)

An experimental deferred rendering pipeline for [jmonkeyengine 3](http://jmonkeyengine.org).

[![](http://img.youtube.com/vi/VcggFR0hMuA/0.jpg)](
https://www.youtube.com/watch?v=VcggFR0hMuA&index=8&list=PLR6cFelWHJZ1-GbCoDQ7gxT3Q7qqyrmYE)


# DONE (+/-)

* store normal as Oct16, based on [A Survey of Efficient Representations for Independent Unit Vectors](http://jcgt.org/published/0003/02/01/) ~ 2014
* use [Scalable Ambient Obscurance (SAO)](http://graphics.cs.williams.edu/papers/SAOHPG12/) ~2012 (I adapted G3D shader from sample), including blur (H+V)
* lights are defined as geometries + use stencil buffer to restrict area of effects.
* use material Id (+ table) to define some materials (diffuse, ...)
* display intermediate data textures (to help for debug) :  normals, depths, matIds, materials table, ao,

# TODO

* generate final result
* add more kind of lights (points, spotlight, directionnals, ambiants) (WIP)
* add "brdf" lighting
* try accumulated light buffer (ignoring material/surface)
* add NPR samples
* optimisation
  * profiler (jvisualvm, mat, ...)
  * bench : via [jmh](https://github.com/melix/jmh-gradle-plugin)
* clean-up
* modularize shader to ease reuse, customisation,...
* basic documentation (README, usages)
* provide doc/sample how to
  * change lighting function
  * add light type
  * extends GBuffer to store additionnal data
  * extends MatIdManager to store additionnal data
* add shadow
  * cast-received shadow (hybrid map + volume) ?
* try [mssao](http://www.comp.nus.edu.sg/~lowkl/publications/mssao_cgi2011.pdf)
* use deep GBuffer based on 
  * update SAO
  * add  pseudo-radiosity (??)
  * add Motion Blur
* a lot of more ...
* support env without depth24stencil8, some ways to explore :
  * http://en.wikibooks.org/wiki/OpenGL_Programming/Stencil_buffer
  * https://www.opengl.org/wiki/Framebuffer_Object_Examples#Stencil
  * https://www.khronos.org/registry/gles/extensions/OES/OES_packed_depth_stencil.txt
  * https://www.opengl.org/registry/specs/EXT/packed_depth_stencil.txt
  * using STENCIL_INDEX8 https://www.khronos.org/registry/gles/extensions/OES/OES_texture_stencil8.txt
     https://groups.google.com/forum/#!topic/webgl-dev-list/6pfpr1GW_-g
* support mac osx
  * https://developer.apple.com/opengl/
  * https://developer.apple.com/opengl/capabilities/index.html
