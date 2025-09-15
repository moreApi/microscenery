package anon.simulation

import fromScenery.utils.extensions.times
import graphics.scenery.Box
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.sqrt

class BoxSimulatable(val parent: Box, var maxIntensity: Short = 4000.toShort(), var range: Float = 15f) : Simulatable {

    override fun intensity(pos: Vector3f): Short {
        val modelPos = Vector4f(pos, 1f).mul(Matrix4f(parent.spatial().model).invertAffine())
        val boxMin = Vector3f(-0.5f) * parent.sizes
        val boxMax = Vector3f(0.5f) * parent.sizes

        val dx = listOf(boxMin.x - modelPos.x, 0f, modelPos.x - boxMax.x).max()
        val dy = listOf(boxMin.y - modelPos.y, 0f, modelPos.y - boxMax.y).max()
        val dz = listOf(boxMin.z - modelPos.z, 0f, modelPos.z - boxMax.z).max()
        val dist = sqrt(dx * dx + dy * dy + dz * dz)


        return when {
            dist <= 0f -> maxIntensity
            dist > range -> 0
            else -> ((1f - (dist / range)) * maxIntensity).toInt().toShort()
        }
    }

    companion object {
        fun addTo(box: Box): BoxSimulatable {
            val tmp = BoxSimulatable(box)
            box.addAttribute(Simulatable::class.java, tmp)
            return tmp
        }
    }
}