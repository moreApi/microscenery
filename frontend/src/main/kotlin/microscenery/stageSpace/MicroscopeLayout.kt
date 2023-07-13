package microscenery.stageSpace

import org.joml.Quaternionf
import org.joml.Vector3f

sealed class MicroscopeLayout(val sheet: Axis) {
    /**
     * Rotation from a plane with the z-axis as normal
     */
    abstract fun sheetRotation(): Quaternionf

    class Default(sheet: Axis = Axis.Z) : MicroscopeLayout(sheet) {
        override fun sheetRotation(): Quaternionf {
            return Quaternionf().rotateTo(Axis.Z.vector, sheet.vector)
        }
    }

    /**
     * TODO: leaning of the sheet
     */
    class Scape(sheet: Axis, val camRotationRad: Float) : MicroscopeLayout(sheet) {
        override fun sheetRotation(): Quaternionf {
            val rot = Quaternionf()
                .rotateLocalX(camRotationRad)
                .rotateTo(Axis.Z.vector, sheet.vector)

//            val rot = Quaternionf()
//            when (sheet) {
//                Axis.X -> rot.rotateY(Math.toRadians(90.0).toFloat())
//                Axis.Y -> rot.rotateX(Math.toRadians(90.0).toFloat())
//                Axis.Z -> throw IllegalStateException("Scape system with Z sheet is unknown to the developer. Sry.")
//            }
//            rot.rotateAxis(Math.toRadians(camRotationDeg).toFloat(), Axis.Z.vector)
            return rot
        }
    }

    enum class Axis(val vector: Vector3f) {
        X(Vector3f(1f, 0f, 0f)), Y(Vector3f(0f, 1f, 0f)), Z(Vector3f(0f, 0f, 1f))
    }
}