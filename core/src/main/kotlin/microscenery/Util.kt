@file:Suppress("unused")
@file:JvmName("Util")
@file:JvmMultifileClass

package microscenery

import fromScenery.Settings
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

fun Settings.setVector3(baseName: String, v: Vector3f){
    this.set(baseName+"X", v.x)
    this.set(baseName+"Y", v.y)
    this.set(baseName+"Z", v.z)
}

fun Long.millisToNanos(): Long = this*1000000
fun Long.nanosToMillis(): Long = this/1000000

