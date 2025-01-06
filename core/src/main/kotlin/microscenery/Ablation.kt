@file:Suppress("unused")
@file:JvmName("Ablation")
@file:JvmMultifileClass

package microscenery

import fromScenery.utils.extensions.minus
import fromScenery.utils.extensions.plus
import fromScenery.utils.extensions.times
import microscenery.hardware.MicroscopeHardware
import microscenery.signals.MicroscopeControlSignal
import org.joml.Vector3f

/**
 * Samples a line from [p1] to [p2] along a grid of [precision]
 *
 * @return all points between [p1] and [p2]
 */
fun sampleLineGrid(p1: Vector3f, p2: Vector3f, precision: Vector3f): List<Vector3f>{
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

/**
 * Samples a line from [p1] to [p2] with a step size that is limited by [precision]. Excluding start and end point
 *
 * @return all points between [p1] and [p2]
 */
fun sampleLineSmooth(p1: Vector3f, p2: Vector3f, precision: Vector3f): List<Vector3f>{
    val diff = p2 - p1
    val subDivisions = Vector3f(diff).absolute() / precision
    val leadDim = subDivisions.maxComponent()
    val maxSteps = subDivisions[leadDim].toInt() //round down
    val stepSize = Vector3f(diff) / maxSteps.toFloat()

    val result = mutableListOf<Vector3f>()
    for (i in 1 until maxSteps){
        val p = Vector3f(stepSize).times(i.toFloat()).plus(p1)
        result += p
    }
    return result
}

fun initAblationSettings(){
    MicroscenerySettings.setVector3fIfUnset(Settings.Ablation.PrecisionUM, Vector3f(2f))
    MicroscenerySettings.set(Settings.Ablation.DwellTimeMicroS, 1000L)
    MicroscenerySettings.set(Settings.Ablation.LaserPower, 0f)
    // count time it takes to move towards next point to that points dwell time
    MicroscenerySettings.set(Settings.Ablation.CountMoveTime, true)
    MicroscenerySettings.set(Settings.Ablation.PauseLaserOnMove, false)
    MicroscenerySettings.set(Settings.Ablation.DryRun, true)
    MicroscenerySettings.set(Settings.Ablation.Repetitions, 1)
    MicroscenerySettings.set(Settings.Ablation.StartAcquisitionAfter, false)
}

/**
 * Uses [MicroscenerySettings] to map [points] to [MicroscopeControlSignal.AblationPoints]
 */
fun buildLaserPath(points: List<Vector3f>): MicroscopeControlSignal.AblationPoints {
    val dwellTime = MicroscenerySettings.get(Settings.Ablation.Repetitions, 1L) //todo this is not correct but for zen studz workaround
    val laserPower = MicroscenerySettings.get(Settings.Ablation.LaserPower, 0f)
    // count time it takes to move towards next point to that points dwell time
    val countMoveTime = MicroscenerySettings.get(Settings.Ablation.CountMoveTime, true)
    val pauseLaserOnMove = MicroscenerySettings.get(Settings.Ablation.PauseLaserOnMove, false)
    val dryRun = MicroscenerySettings.get(Settings.Ablation.DryRun, true)

    return MicroscopeControlSignal.AblationPoints(points.mapIndexed { index, vector3f ->
        MicroscopeControlSignal.AblationPoint(
            vector3f,
            dwellTime,
            (index == 0 || pauseLaserOnMove) && !dryRun,
            (index == points.size -1 || pauseLaserOnMove) && !dryRun,
            if (dryRun) 0f else laserPower,
            countMoveTime
        )
    })
}

//TODO move to MMMicroscope
fun executeAblationCommandSequence(hardware: MicroscopeHardware, signal: MicroscopeControlSignal.AblationPoints) {
    val dryRun = MicroscenerySettings.get(Settings.Ablation.DryRun, true)
    val repetitions = MicroscenerySettings.get(Settings.Ablation.Repetitions, 1)
    val startAcquisitionAfter = MicroscenerySettings.get(Settings.Ablation.StartAcquisitionAfter, false)

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