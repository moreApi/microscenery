package microscenery.scenes.stageStudy

import fromScenery.utils.extensions.minus
import graphics.scenery.utils.lazyLogger
import microscenery.scenes.stageStudy.Orchestration.TrialCoordinator
import microscenery.simulation.ProceduralBlob
import microscenery.simulation.StageSimulation.Companion.showMaterial
import org.joml.Vector3f

class TargetJudge(targetBlobs: List<ProceduralBlob>, val studySpatialLogger: StudySpatialLogger, val trialCoordinator: TrialCoordinator?) {
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
                closest.second.key.showMaterial()
                 if (!targets.any{!it.value}){
                    Results.AllHit
                } else {
                    Results.Hit
                }
            }
        }

        studySpatialLogger.logEvent("MarkRoi")
        logger.warn("got a  " + result.toString())
        if (result == TargetJudge.Results.AllHit) trialCoordinator?.caseFinished(false)

        return result
    }
}