package microscenery.UI

import graphics.scenery.Node
import graphics.scenery.Scene
import graphics.scenery.controls.InputHandler
import graphics.scenery.volumes.Volume
import microscenery.MicrosceneryHub
import microscenery.stageSpace.SliceRenderNode
import org.scijava.ui.behaviour.ClickBehaviour

object DesktopUI {
    fun initMouseSelection(inputHandler: InputHandler?, msHub: MicrosceneryHub){
        inputHandler ?: return

        val name = "selectNodeWithMouse"
        val scene = msHub.getAttribute(Scene::class.java)
        val uiModel = msHub.getAttribute(UIModel::class.java)
        inputHandler.addBehaviour(name, object : ClickBehaviour{
            override fun click(x: Int, y: Int) {
                scene.activeObserver?.let { cam ->
                        val matches = cam.getNodesForScreenSpacePosition(x, y, { isValidSelectionTarget(it)} , false)
                        uiModel.selected = matches.matches.firstOrNull()?.node ?: uiModel.NO_SELECTION
                    }
                }
        })
        inputHandler.addKeyBinding( name, "1")
    }

    private fun isValidSelectionTarget(node: Node) : Boolean = when{
        node is Volume -> true
        node is SliceRenderNode -> true
        else -> false
    }
}