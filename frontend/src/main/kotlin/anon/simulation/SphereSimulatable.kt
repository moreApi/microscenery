package anon.simulation

import graphics.scenery.Sphere
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f

class SphereSimulatable(val parent: Sphere, var maxIntensity: Short = 4000.toShort(), var range: Float = 5f) :
    Simulatable {

    override fun intensity(pos: Vector3f): Short {
        val modelPos = Vector4f(pos, 1f).mul(Matrix4f(parent.spatial().model).invertAffine())

        val dist = modelPos.length()


        return when {
            dist <= parent.radius -> maxIntensity
            dist > parent.radius + range -> 0
            else -> ((1f - (dist / (parent.radius + range))) * maxIntensity).toInt().toShort()
        }
    }

    companion object {
        fun addTo(box: Sphere): SphereSimulatable {
            val tmp = SphereSimulatable(box)
            box.addAttribute(Simulatable::class.java, tmp)
            return tmp
        }
    }
}