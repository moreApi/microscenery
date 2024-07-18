package microscenery.VRUI

import graphics.scenery.attribute.spatial.HasSpatial
import graphics.scenery.attribute.spatial.Spatial
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackerRole
import graphics.scenery.controls.behaviours.PerButtonPressable
import graphics.scenery.controls.behaviours.Pressable
import graphics.scenery.controls.behaviours.SimplePressable
import graphics.scenery.controls.behaviours.Touchable
import microscenery.UI.UIModel
import microscenery.detach
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.PI

interface VRHandTool : HasSpatial {

    fun getTipCollider(): Spatial? = null

    fun activate(uiModel: UIModel, side: TrackerRole) {
        uiModel.inHand(side)?.deactivate(uiModel)
        this.deactivate(uiModel) // in case tool is in other hand. we are not dual wielding tools here

        val controller = uiModel.getHandController(side) ?: return
        spatial {
            rotation = Quaternionf().rotationX(-PI.toFloat()*0.5f)
            position = Vector3f()
        }
        controller.model?.addChild(this)
        uiModel.putInHand(side, this)
    }

    fun deactivate(uiModel: UIModel) {
        listOf(TrackerRole.LeftHand, TrackerRole.RightHand).forEach { side ->
            if (uiModel.inHand(side) == this) {
                uiModel.putInHand(side, null)
                this.detach()
            }
        }
    }

    /**
     * Change Side button to grab functionality and set behavior active TODO
     */
    fun initVRHandToolAndPressable(uiModel: UIModel, pressable: PerButtonPressable?) {
        val actionsWithoutSide = pressable?.actions?.minus(OpenVRHMD.OpenVRButton.Side) ?: emptyMap()
        this.addAttribute(Pressable::class.java, PerButtonPressable(actionsWithoutSide))
    }
}