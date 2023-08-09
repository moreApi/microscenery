package microscenery

import fromScenery.utils.extensions.xyz
import graphics.scenery.Box
import graphics.scenery.Node
import graphics.scenery.Scene
import graphics.scenery.controls.OpenVRHMD
import microscenery.VRUI.behaviors.AnalogInputWrapper
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
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

/**
 * @return corners of clipping rectangle in world space
 */
fun Box.intersectZPlane(zPlane: Float): List<Vector3f>?{
    // origin of a box is in the middle
    val spatial = this.spatial()

    // MS = model space of box
    val planeMS4f = Vector4f(spatial.worldPosition(), 1f)
    planeMS4f.z = zPlane
    planeMS4f.mul(spatial.world.copy().invert())

    if (planeMS4f.z > sizes.z * 0.5 || planeMS4f.z < sizes.z * -0.5) {
        // box and plane do not intersect
        return null
    }

    val planeMS3f = planeMS4f.xyz()

    val tl = Vector2f(-0.5f * sizes.x(), -0.5f * sizes.y())
    val br = Vector2f( 0.5f * sizes.x() , 0.5f * sizes.y())


    val cornersMS = listOf(
        Vector3f(tl, 0f),
        Vector3f(br.x,tl.y,0f),
        Vector3f(br,0f),
        Vector3f(tl.x, br.y, 0f)
    )
    //todo
    return emptyList()

}