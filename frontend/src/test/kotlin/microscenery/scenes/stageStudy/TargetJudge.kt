package microscenery.scenes.stageStudy

import fromScenery.utils.extensions.minus
import graphics.scenery.utils.lazyLogger
import microscenery.simulation.ProceduralBlob
import microscenery.simulation.StageSimulation.Companion.showMaterial
import org.joml.Vector3f
import kotlin.system.exitProcess

class TargetJudge(targetBlobs: List<ProceduralBlob>, val studySpatialLogger: StudySpatialLogger) {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))
    var hitRadius = 35f

    var targets: MutableMap<ProceduralBlob,Boolean>


    init {
        targets = targetBlobs.associateWith { false }.toMutableMap()
    }

    enum class Results{NoHit,Hit, AlreadyHit,AllHit}
    fun hit(pos: Vector3f): Results{
        val closest = targets.map { (it.key.spatial().position - pos).length() to it }.minByOrNull { it.first } ?: return Results.NoHit

        val result = when{
            closest.first > hitRadius ->  Results.NoHit
            closest.second.value ->  Results.AlreadyHit
            else -> {
                targets[closest.second.key] = true
                 if (!targets.any{!it.value}){
                    Results.AllHit

                } else {
                    closest.second.key.showMaterial()
                    Results.Hit
                }
            }
        }

        studySpatialLogger.logEvent("MarkRoi")
        logger.warn("got a  " + result.toString())
        if (result == TargetJudge.Results.AllHit) exitProcess(0)

        return result
    }
}