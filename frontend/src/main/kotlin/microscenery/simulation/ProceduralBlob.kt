package microscenery.simulation

import fromScenery.utils.extensions.minus
import graphics.scenery.Sphere
import graphics.scenery.attribute.material.Material
import org.joml.Vector3f

class ProceduralBlob(val procedural: Procedural = Procedural(use16bit = true), var maxIntensity: Short = 4000) :
    Sphere(radius = procedural.size / 2f), Simulatable {

    constructor(size: Int, maxIntensity: Short = 4000) : this(Procedural(size, use16bit = true), maxIntensity)

    init {
        name = "procedural Blob"
        material().cullingMode = Material.CullingMode.FrontAndBack
        addAttribute(Simulatable::class.java, this)
    }

    val size = procedural.size
    override fun intensity(pos: Vector3f): Short {
        val localPos = pos - spatial().position
        if (localPos.length() > size / 2) {
            // out of range
            return 0
        }
        return ((procedural.point(localPos) / procedural.range.toFloat()) * maxIntensity.toFloat()).toInt().toShort()
            .coerceAtLeast(0)
    }
}