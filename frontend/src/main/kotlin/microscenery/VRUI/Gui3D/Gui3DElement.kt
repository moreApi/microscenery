package microscenery.VRUI.Gui3D

import fromScenery.utils.extensions.minus
import fromScenery.utils.extensions.plus
import graphics.scenery.attribute.geometry.HasGeometry
import graphics.scenery.attribute.spatial.HasSpatial
import graphics.scenery.controls.behaviours.Grabable
import org.joml.Vector3f

/**
 * REMEMBER TO [initGrabable]
 */
interface Gui3DElement: HasSpatial {
    /**
     * Assuming no scaling
     */
    val width: Float

    /**
     * Assuming no scaling
     */
    val height: Float

    /**
     * Sets a grabable that moves the root [VR3DGui] instead and saves the last position as menu offset
     */
    fun initGrabable(node: HasGeometry){
        var grab: Grabable? = null
        var start = Vector3f()
        grab = Grabable(onGrab = {
            var cur = node.parent
            while (cur != null && cur !is VR3DGui.VR3DGuiRootNode) cur = cur.parent
            if (cur is VR3DGui.VR3DGuiRootNode){
                grab?.target = {cur}
                cur.ifSpatial {
                    start = position
                }
            }
        }, onRelease = {
            val rn = grab?.target?.let { it() as? VR3DGui.VR3DGuiRootNode} ?: return@Grabable
            val diff = rn.spatial().position - start
            rn.owner.offset += diff
        }, lockRotation = true)
        node.addAttribute(Grabable::class.java,grab)
    }

}