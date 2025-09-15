package anon.UI

import graphics.scenery.Node
import graphics.scenery.Scene
import graphics.scenery.controls.InputHandler
import graphics.scenery.controls.behaviours.ArcballCameraControl
import graphics.scenery.volumes.Volume
import anon.MicrosceneryHub
import anon.stageSpace.SliceRenderNode
import anon.stageSpace.StageSpaceManager
import org.scijava.ui.behaviour.ClickBehaviour

object DesktopUI {
    fun initMouse(inputHandler: InputHandler?, msHub: MicrosceneryHub){
        inputHandler ?: return

        val name = "selectNodeWithMouse"
        val scene = msHub.getAttribute(Scene::class.java)
        val uiModel = msHub.getAttribute(UIModel::class.java)
        inputHandler.addBehaviour(name, object : ClickBehaviour{
            override fun click(x: Int, y: Int) {
                scene.activeObserver?.let { cam ->
                        val matches = cam.getNodesForScreenSpacePosition(x, y, { isValidSelectionTarget(it)} , false)
                        uiModel.selected = matches.matches.firstOrNull()?.node
                    }
                }
        })
        inputHandler.addKeyBinding( name, "button1")

        // remove FPS camera on mouse button 1
        inputHandler.removeKeyBinding("mouse_control")

        val stageSpaceManager =  msHub.getAttribute(StageSpaceManager::class.java)
        val targetArcball =
            ArcballCameraControl("mouse_control", { scene.findObserver() }, 500, 500, {
                uiModel.selected?.spatialOrNull()?.worldPosition()
                    ?: stageSpaceManager.focusManager.focusTarget.spatial().worldPosition()
            })

        inputHandler.addBehaviour("arcCam", targetArcball)
        inputHandler.addKeyBinding("arcCam", "button2")
    }

    private fun isValidSelectionTarget(node: Node) : Boolean = when{
        node is Volume -> true
        node is SliceRenderNode -> true
        else -> false
    }
}