An experimental deferred rendering pipeline for [jmonkeyengine 3](http://jmonkeyengine.org).

https://www.youtube.com/watch?v=lDPRoRGYWnY


# DONE (+/-)

* store normal as Oct16, based on [A Survey of Efficient Representations for Independent Unit Vectors](http://jcgt.org/published/0003/02/01/) ~ 2014
* use [Scalable Ambient Obscurance (SAO)](http://graphics.cs.williams.edu/papers/SAOHPG12/) ~2012 (I adapted G3D shader from sample), including blur (H+V)
* lights are defined as geometries + use stencil buffer to restrict area of effects.
* use material Id (+ table) to define some materials (diffuse, ...)
* display intermediate data textures (to help for debug) :  normals, depths, matIds, materials table, ao,

# TODO

* generate final result
* add more kind of lights (points, spotlight, directionnals, ambiants)
* add "brdf" lighting
* try accumulated light buffer (ignoring material/surface)
* add NPR samples
* optimisation
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
* use deep GBuffer based on 
  * update SAO
  * add  pseudo-radiosity (??)
  * add Motion Blur
* a lot of more ...
