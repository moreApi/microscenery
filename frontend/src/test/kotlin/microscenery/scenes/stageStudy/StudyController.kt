package microscenery.scenes.stageStudy

import fromScenery.utils.extensions.minus
import microscenery.simulation.ProceduralBlob
import microscenery.simulation.StageSimulation.Companion.showMaterial
import org.joml.Vector3f

class StudyController(targetBlobs: List<ProceduralBlob>) {
    var hitRadius = 35f

    var targets: MutableMap<ProceduralBlob,Boolean>


    init {
        targets = targetBlobs.associateWith { false }.toMutableMap()
    }

    enum class Results{NoHit,Hit, AlreadyHit,AllHit}
    fun hit(pos: Vector3f): Results{
        val closest = targets.map { (it.key.spatial().position - pos).length() to it }.minByOrNull { it.first } ?: return Results.NoHit

        when{
            closest.first > hitRadius -> return Results.NoHit
            closest.second.value -> return Results.AlreadyHit
            else -> {
                targets[closest.second.key] = true
                return if (!targets.any{!it.value}){
                    Results.AllHit
                } else {
                    closest.second.key.showMaterial()
                    Results.Hit
                }
            }
        }
    }
}