package microscenery.VRUI

import graphics.scenery.attribute.spatial.HasSpatial
import graphics.scenery.attribute.spatial.Spatial
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.behaviours.PerButtonPressable
import graphics.scenery.controls.behaviours.Pressable
import graphics.scenery.controls.behaviours.SimplePressable
import graphics.scenery.controls.behaviours.Touchable
import microscenery.UI.UIModel
import microscenery.detach
import org.joml.Vector3f

interface VRHandTool: HasSpatial {

    fun getTipCollider(): Spatial? = null

    /**
     * Change Side button to grab functionality and set behavior active
     */
    fun initVRHandToolAndPressable(uiModel: UIModel, pressable: PerButtonPressable?){

        this.addAttribute(Touchable::class.java, Touchable())

        val actionsWithoutSide = pressable?.actions?.minus(OpenVRHMD.OpenVRButton.Side) ?: emptyMap()
        this.addAttribute(
            Pressable::class.java, PerButtonPressable(actionsWithoutSide.plus(
                OpenVRHMD.OpenVRButton.Side to SimplePressable(onPress = { controllerSpatial, device ->
                    when(uiModel.inHand(device.role)){
                        null -> {
                            // put into hand
                            uiModel.putInHand(device.role, this)
                            spatial{
                                rotation.premul(controllerSpatial.worldRotation().invert())
                                position = Vector3f()
                            }
                            device.model?.addChild(this)
                        }
                        this -> {
                            // remove from hand
                            uiModel.putInHand(device.role, null)
                            spatial{
                                position = controllerSpatial.worldPosition()
                                rotation = worldRotation()
                            }
                            this.detach()
                        }
                        else -> {
                            // holding other tool - this should not happen
                            throw IllegalStateException("Picking up tool while holding one.")
                        }
                    }
                })
            ))
        )
    }
}