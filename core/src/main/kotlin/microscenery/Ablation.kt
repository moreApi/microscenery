@file:Suppress("unused")
@file:JvmName("Ablation")
@file:JvmMultifileClass

package microscenery

import fromScenery.utils.extensions.minus
import fromScenery.utils.extensions.times
import microscenery.signals.ClientSignal
import org.joml.Vector3f

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

/**
 * Uses [MicroscenerySettings] to map [points] to [ClientSignal.AblationPoints]
 */
fun buildLaserPath(points: List<Vector3f>): ClientSignal.AblationPoints{
    val dwellTime = MicroscenerySettings.get("Ablation.dwellTimeMillis", 0L)
    val laserPower = MicroscenerySettings.get("Ablation.laserPower", 0f)
    // count time it takes to move towards next point to that points dwell time
    val countMoveTime = MicroscenerySettings.get("Ablation.CountMoveTime", true)
    val pauseLaserOnMove = MicroscenerySettings.get("Ablation.PauseLaserOnMove", false)
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

    //label total time
    //recorded time - label
    //        do dry run
}