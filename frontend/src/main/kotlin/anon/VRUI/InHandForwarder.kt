package anon.VRUI

import graphics.scenery.Scene
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackerRole
import graphics.scenery.controls.behaviours.VRPress
import anon.UI.UIModel
import org.scijava.ui.behaviour.DragBehaviour
import kotlin.concurrent.thread

/**
 * Redirects button presses to hand held tools if present to circumvent imprecise collision detection.
 */
class InHandForwarder(
    val side: TrackerRole,
    val button: OpenVRHMD.OpenVRButton,
    val pressBehavior: VRPress,
    val uiModel: UIModel
) :
    DragBehaviour {
    init {
        if (side == TrackerRole.Invalid) throw IllegalStateException()
    }

    override fun init(x: Int, y: Int) {
        val inHand = uiModel.inHand(side)
        if (inHand != null) {
            pressBehavior.initFor(inHand)
        } else {
            pressBehavior.init(0, 0)
        }
    }

    override fun drag(x: Int, y: Int) {
        val inHand = uiModel.inHand(side)
        if (inHand != null) {
            pressBehavior.dragFor(inHand)
        } else {
            pressBehavior.drag(0, 0)
        }
    }

    override fun end(x: Int, y: Int) {
        val inHand = uiModel.inHand(side)
        if (inHand != null) {
            pressBehavior.endFor(inHand)
        } else {
            pressBehavior.end(0, 0)
        }
    }

    companion object {
        fun createAndWrapVRPressWithInHandManagerBehavior(
            uiModel: UIModel,
            scene: Scene,
            hmd: OpenVRHMD,
            side: TrackerRole,
            buttons: List<OpenVRHMD.OpenVRButton>
        ) {
            thread {
                VRPress.createAndSet(
                    scene,
                    hmd,
                    buttons,
                    side
                ).get().forEach {
                    hmd.removeKeyBinding(it.name)
                    hmd.removeBehaviour(it.name)
                    val name = it.name + ":InHandManager"
                    hmd.addBehaviour(name, InHandForwarder(side, it.button, it, uiModel))
                    hmd.addKeyBinding(name, side, it.button)
                }

            }
        }
    }
}