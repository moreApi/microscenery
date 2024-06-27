package microscenery.UI

import fromScenery.utils.extensions.minus
import fromScenery.utils.extensions.plus
import fromScenery.utils.extensions.times
import graphics.scenery.Scene
import microscenery.MicrosceneryHub
import microscenery.MicroscenerySettings
import microscenery.Settings
import microscenery.UP
import microscenery.stageSpace.SliceRenderNode
import microscenery.stageSpace.StageSpaceManager
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.PI

class ScapeViewerUI(val msHub: MicrosceneryHub) {
    val scene = msHub.getAttribute(Scene::class.java)
    val uiModel = msHub.getAttribute(UIModel::class.java)
    val stageSpaceManager = msHub.getAttribute(StageSpaceManager::class.java)

    init {
        //rotate the rotation pivot to fit [this.setViewDirection]
        stageSpaceManager.stageRoot.parent?.ifSpatial{
            this.rotation = Quaternionf().rotationX(0.5f* PI.toFloat())
        }
    }

    /**
     * @param direction vector of length 1
     */
    fun setViewDirection(direction: Vector3f){
        val cam = scene.findObserver() ?: throw IllegalStateException("Cant find Camera.")
        val pivot = uiModel.selected?.spatialOrNull()?.worldPosition() ?: middleOfSlices()
        val distance = (pivot - cam.spatial().worldPosition()).length()

        cam.spatial {
            position = pivot + direction * distance * -1f
            rotation = Quaternionf().lookAlong(direction, Vector3f(0f,1f,0f))
        }

    }

    private fun middleOfSlices(): Vector3f{
        val l = scene.discover(scene,{it is SliceRenderNode })
            .map { it as SliceRenderNode }
            .toList()
        return l.fold(Vector3f()) { acc, sliceRenderNode -> acc + sliceRenderNode.spatial().worldPosition()}
            .div(l.size.toFloat())
    }

    fun resetView(){
        val cam = scene.findObserver() ?: throw IllegalStateException("Cant find Camera.")

        cam.spatial {
            position = Vector3f(0.0f, 1.5f, 1.5f)
            rotation = Quaternionf().lookAlong(Vector3f(0.0f, -1.5f, -1.5f), UP)
        }
    }

    companion object{
        fun scapeViewerSettings(){
            MicroscenerySettings.set(Settings.StageSpace.HideFocusFrame, false)
            MicroscenerySettings.set(Settings.StageSpace.HideFocusTargetFrame, false)
            MicroscenerySettings.set(Settings.StageSpace.HideStageSpaceLabel, true)
            MicroscenerySettings.set(Settings.StageSpace.RandomSliceOffset, 0.1f)
            MicroscenerySettings.set(Settings.UI.ShowBorderOfSelected,true)
            MicroscenerySettings.set(Settings.UI.ShowSelectionIndicator,false)
        }
    }
}