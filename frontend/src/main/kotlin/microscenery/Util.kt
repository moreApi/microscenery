package microscenery

import graphics.scenery.Node
import graphics.scenery.Scene
import graphics.scenery.attribute.material.HasMaterial
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.behaviours.Touchable
import microscenery.VRUI.behaviors.AnalogInputWrapper
import org.joml.Vector3f
import org.scijava.ui.behaviour.Behaviour
import org.scijava.ui.behaviour.DragBehaviour


fun wrapForAnalogInputIfNeeded(
    scene: Scene,
    button: OpenVRHMD.OpenVRButton,
    behavior: DragBehaviour
): Behaviour {

    val analogButtons = listOf(
        OpenVRHMD.OpenVRButton.Up,
        OpenVRHMD.OpenVRButton.Down,
        OpenVRHMD.OpenVRButton.Left,
        OpenVRHMD.OpenVRButton.Right
    )
    return if (button in analogButtons)
        AnalogInputWrapper(behavior, scene)
    else
        behavior
}

/**
 * Remove node from parent if there is one.
 */
fun Node.detach(){
    this.parent?.removeChild(this)
}

fun HasMaterial.changeColorWithTouchable(newColor: Vector3f){
    val touch = this.getAttributeOrNull(Touchable::class.java)
    if (touch?.originalDiffuse != null){
        // this might screw with [VRTouch]s coloring
        touch.originalDiffuse = newColor
    } else {
        this.material().diffuse = newColor
    }
}