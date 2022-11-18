package microscenery.stageSpace

import org.joml.Quaternionf
import org.joml.Vector3f

sealed class MicroscopeLayout {
    /**
     * Rotation from a plane with the z-axis as normal
     */
    abstract fun sheetRotation(): Quaternionf

    class Default(val sheet: Axis = Axis.Z) : MicroscopeLayout() {
        override fun sheetRotation(): Quaternionf {
            return Quaternionf().rotateTo(Axis.Z.vector, sheet.vector)
        }
    }

    /**
     * @param degree leaning towards the positive side of the axis in angular
     */
    class Scape(val sheet: Axis, val degree: Float) : MicroscopeLayout() {
        override fun sheetRotation(): Quaternionf {
            val rot = Quaternionf()
            when(sheet){
                Axis.X -> rot.rotateZ(Math.toRadians(degree.toDouble()).toFloat())
                Axis.Y -> rot.rotateX(Math.toRadians(degree.toDouble()).toFloat())
                Axis.Z -> throw IllegalStateException("Scape system with Z sheet is unknown to the developer. Sry.")
            }
            return rot.rotateTo(Axis.Z.vector, sheet.vector)
        }
    }

    @Suppress("unused")
    enum class Axis(val vector: Vector3f) {
        X(Vector3f(1f, 0f, 0f)), Y(Vector3f(0f, 1f, 0f)), Z(Vector3f(0f, 0f, 1f))
    }
}