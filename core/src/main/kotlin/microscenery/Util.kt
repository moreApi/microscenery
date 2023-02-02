@file:Suppress("unused")
@file:JvmName("Util")
@file:JvmMultifileClass

package microscenery

import fromScenery.Settings
import fromScenery.utils.extensions.minus
import fromScenery.utils.extensions.times
import microscenery.signals.MicroscopeSignal
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit


val MicroscenerySettings = Settings(prefix = "microscenery.", propertiesFilePath = "microscenery.properties")

val UP = Vector3f(0f, 1f, 0f)

fun <T, U, W> T?.let(any: U?, call: (T, U) -> W): W? =
    if (this != null && any != null)
        call(this, any)
    else
        null


/**
 * sleep but continue once a value is there. Should speed up tests
 */
fun lightSleepOnNull(mills: Int = 10000, target: () -> Any?) {
    for (t in 1..10) {
        if (target() == null)
            Thread.sleep(mills / 10L)
    }
}

/**
 * sleep but continue once a condition is met. Should speed up tests
 */
fun lightSleepOnCondition(mills: Int = 10000, target: () -> Boolean) {
    for (t in 1..10) {
        if (!target())
            Thread.sleep(mills / 10L)
    }
}

inline fun <reified T : MicroscopeSignal> BlockingQueue<MicroscopeSignal>.pollForSignal(
    timeout: Long = 5000,
    ignoreNotFitting: Boolean = true,
    condition: (T) -> Boolean = { true }
): Boolean {
    val start = System.currentTimeMillis()
    while (start + timeout > System.currentTimeMillis()) {
        val signal = this.poll(200, TimeUnit.MILLISECONDS) as? T ?: continue
        if (condition(signal))
            return true
        else if (!ignoreNotFitting)
            return false
    }
    return false
}

fun nowMillis(): Long = System.currentTimeMillis()

fun Matrix4f.copy(): Matrix4f = Matrix4f(this)

fun Vector3f.toVector4f(w: Float): Vector4f = Vector4f(this, w)
fun Vector4f.toVector3f(): Vector3f = Vector3f(x, y, z)

fun Vector3f.toReadableString() = String.format("(%.3f,%.3f,%.3f)", x, y, z)
fun Vector4f.toReadableString() = String.format("(%.3f,%.3f,%.3f,%.3f)", x, y, z, w)


/**
 * Returns true if this is less than to in every dimension.
 */
fun Vector3f.isFullyLessThan(to: Vector3f): Boolean {
    return this.x < to.x && this.y < to.y && this.z < to.z
}

fun Settings.getVector3(baseName: String): Vector3f?{
    return Vector3f(
        getOrNull(baseName+"X") ?: return null,
        getOrNull(baseName+"Y") ?: return null,
        getOrNull(baseName+"Z") ?: return null
    )
}

fun Settings.setVector3fIfUnset(baseName: String, v: Vector3f){
    this.setIfUnset(baseName+"X", v.x)
    this.setIfUnset(baseName+"Y", v.y)
    this.setIfUnset(baseName+"Z", v.z)
}

fun Long.millisToNanos(): Long = this*1000000
fun Long.nanosToMillis(): Long = this/1000000

/**
 * Samples a line between [p1] and [p2] with [precision] steps
 *
 * @return all points between [p1] and [p2]
 */
fun sampleLine(p1: Vector3f, p2: Vector3f, precision: Vector3f): List<Vector3f>{
    val diff = p2 - p1
    val subDivisions = Vector3f(diff).absolute() / precision
    val leadDim = subDivisions.maxComponent()
    val otherDims = listOf(0,1,2).filter { it != leadDim }
    val stepSize = Vector3f(diff) / subDivisions

    val result = mutableListOf<Vector3f>()
    for (i in 1 until subDivisions[leadDim].toInt()){
        val exactPosition = diff * (i / subDivisions[leadDim] )
        val p = Vector3f()
        p.setComponent(leadDim, p1[leadDim] + stepSize[leadDim] * i)
        for (dim in otherDims) {
            val precisionSteps = exactPosition[dim] / precision[dim]
            p.setComponent(dim, p1[dim]
                    + precisionSteps.toInt() * precision[dim]
                    + if(precisionSteps - precisionSteps.toInt() > 0.5f ) precision[dim] else 0f)
        }
        result += p
    }
    return result
}