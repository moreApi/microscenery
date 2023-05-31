@file:Suppress("unused")
@file:JvmName("Ablation")
@file:JvmMultifileClass

package microscenery

import fromScenery.utils.extensions.minus
import fromScenery.utils.extensions.plus
import fromScenery.utils.extensions.times
import microscenery.hardware.MicroscopeHardware
import microscenery.signals.ClientSignal
import org.joml.Vector3f

/**
 * Samples a line from [p1] to [p2] along a grid of [precision]
 *
 * @return all points between [p1] and [p2]
 */
fun sampleLineGrid(p1: Vector3f, p2: Vector3f, precision: Vector3f): List<Vector3f>{
    val diff = p1 - p2
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

/**
 * Samples a line from [p1] to [p2] with a step size that is limited by [precision].
 *
 * @return all points between [p1] and [p2]
 */
fun sampleLineSmooth(p1: Vector3f, p2: Vector3f, precision: Vector3f): List<Vector3f>{
    val diff = p1 - p2
    val subDivisions = Vector3f(diff).absolute() / precision
    val leadDim = subDivisions.maxComponent()
    val stepSize = Vector3f(diff) / subDivisions

    val result = mutableListOf<Vector3f>()
    for (i in 1 until subDivisions[leadDim].toInt()){
        val p = Vector3f(stepSize).times(i.toFloat()).plus(p1)
        result += p
    }
    return result
}

fun initAblationSettings(){
    MicroscenerySettings.setVector3fIfUnset("Ablation.precision", Vector3f(1f))
    MicroscenerySettings.set("Ablation.dwellTimeMillis", 0L)
    MicroscenerySettings.set("Ablation.laserPower", 0f)
    // count time it takes to move towards next point to that points dwell time
    MicroscenerySettings.set("Ablation.countMoveTime", true)
    MicroscenerySettings.set("Ablation.pauseLaserOnMove", false)
    MicroscenerySettings.set("Ablation.dryRun", true)
    MicroscenerySettings.set("Ablation.repetitions", 1)
    MicroscenerySettings.set("Ablation.startAcquisitionAfter", false)
}

/**
 * Uses [MicroscenerySettings] to map [points] to [ClientSignal.AblationPoints]
 */
fun buildLaserPath(points: List<Vector3f>): ClientSignal.AblationPoints{
    val dwellTime = MicroscenerySettings.get("Ablation.dwellTimeMillis", 0L)
    val laserPower = MicroscenerySettings.get("Ablation.laserPower", 0f)
    // count time it takes to move towards next point to that points dwell time
    val countMoveTime = MicroscenerySettings.get("Ablation.countMoveTime", true)
    val pauseLaserOnMove = MicroscenerySettings.get("Ablation.pauseLaserOnMove", false)
    val dryRun = MicroscenerySettings.get("Ablation.dryRun", true)

    return ClientSignal.AblationPoints(points.mapIndexed { index, vector3f ->
        ClientSignal.AblationPoint(
            vector3f,
            dwellTime.millisToNanos(),
            (index == 0 || pauseLaserOnMove) && !dryRun,
            (index == points.size -1 || pauseLaserOnMove) && !dryRun,
            if (dryRun) 0f else laserPower,
            countMoveTime
        )
    })
}

fun executeAblationCommandSequence(hardware: MicroscopeHardware, signal: ClientSignal.AblationPoints ){
    val dryRun = MicroscenerySettings.get("Ablation.dryRun", true)
    val repetitions = MicroscenerySettings.get("Ablation.repetitions", 1)
    val startAcquisitionAfter = MicroscenerySettings.get("Ablation.startAcquisitionAfter", false)

    hardware.stagePosition = signal.points.first().position
    hardware.ablatePoints(signal)
    if (!dryRun) {
        for (r in 2..repetitions) {
            hardware.ablatePoints(signal)
        }
    }
    if (startAcquisitionAfter){
        hardware.startAcquisition()
    }
}