package microscenery.simulation

import fromScenery.utils.extensions.minus
import graphics.scenery.Sphere
import graphics.scenery.attribute.material.Material
import org.joml.Vector3f
import kotlin.math.pow

class ProceduralBlob(
    val procedural: Procedural = Procedural(use16bit = true),
    val maxIntensity: Short = 4000,
    val intensityOffset: Short = 20000
) :
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
        val dist = localPos.length()
        val relativeDist = (dist / (size / 2))
        if (dist > size / 2) {
            // out of range
            return 0
        }
        val proceduralValue =
            ((procedural.point(localPos) / procedural.range.toFloat()) * maxIntensity.toFloat()).toInt().toShort()
                .coerceAtLeast(0) * if (relativeDist < 0.8f) 1f else (1 - (relativeDist - 0.8f) / 0.2f)
        val coreValue = (relativeDist - 1).pow(2) * maxIntensity


        return ((proceduralValue + coreValue).toInt().toShort().coerceAtMost(maxIntensity) + intensityOffset).toShort()
    }
}