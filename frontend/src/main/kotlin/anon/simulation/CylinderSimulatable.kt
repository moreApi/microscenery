package anon.simulation

import graphics.scenery.attribute.spatial.Spatial
import graphics.scenery.primitives.Cylinder
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.absoluteValue

class CylinderSimulatable(
    val parent: Cylinder,
    var maxIntensity: Short = 4000.toShort(),
    var range: Float = 15f,
    val hollow: Boolean = true,
    val stageRoot: Spatial
) : Simulatable {

    override fun intensity(pos: Vector3f): Short {
        val modelPos = Vector4f(pos, 1f).mul(parent.inverseStageMatrix(stageRoot = stageRoot))

        var distanceFromDisc = Vector2f(modelPos.x, modelPos.z).length() - parent.radius
        if (distanceFromDisc.absoluteValue > range) return 0 //out of range
        distanceFromDisc = if (!hollow) {
            distanceFromDisc.coerceAtLeast(0f)
        } else {
            distanceFromDisc.absoluteValue
        }
        val valueXY = (1 - distanceFromDisc / range) * maxIntensity

        val modY = when {
            modelPos.y < -range -> 0f
            modelPos.y < 0 -> 1 - modelPos.y.absoluteValue / range
            modelPos.y < parent.height -> 1f
            modelPos.y - parent.height < range -> 1 - (modelPos.y.absoluteValue - parent.height) / range
            else -> 0f // case: modelPos.y - parent.height > range
        }

        return (valueXY * modY).toInt().toShort()
    }

    companion object {
        fun addTo(box: Cylinder, stageRoot: Spatial): CylinderSimulatable {
            val tmp = CylinderSimulatable(box, stageRoot = stageRoot)
            box.addAttribute(Simulatable::class.java, tmp)
            return tmp
        }
    }
}