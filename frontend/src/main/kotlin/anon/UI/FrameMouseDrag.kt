package anon.UI

import fromScenery.utils.extensions.times
import graphics.scenery.Camera
import graphics.scenery.attribute.spatial.HasSpatial
import graphics.scenery.utils.lazyLogger
import anon.UP
import org.joml.Vector3f
import org.scijava.ui.behaviour.DragBehaviour
import org.scijava.ui.behaviour.ScrollBehaviour

/**
 *
 * Liberally adapted from [MouseDragPlane]
 * @author Jan Tiemann
 */
open class FrameMouseDrag(
    protected val targetNode: HasSpatial,
    protected val camera: Camera,
    protected val mouseSpeed: () -> Float = { 0.25f },
    protected val fpsSpeedSlow: () -> Float = { 0.05f },
    val name: String = "Frame Mouse Drag"
) : DragBehaviour, ScrollBehaviour{

    protected val logger by lazyLogger()

    private var lastX = 0
    private var lastY = 0


    override fun init(x: Int, y: Int) {
        lastX = x
        lastY = y
    }

    override fun drag(x: Int, y: Int) {

        if (!targetNode.lock.tryLock()) return

        targetNode.ifSpatial {
            val dragPosUpdater = Vector3f(if (camera.right.x > 0f) 1f else -1f,0f,0f) * ((x - lastX) * fpsSpeedSlow() * mouseSpeed())
            position.add(dragPosUpdater)
            UP.mul((lastY - y) * fpsSpeedSlow() * mouseSpeed(), dragPosUpdater)
            position.add(dragPosUpdater)
            needsUpdate = true
        }

        targetNode.lock.unlock()

        lastX = x
        lastY = y

    }

    override fun end(x: Int, y: Int) {
        // intentionally empty. A new click will overwrite the running variables.
    }

    override fun scroll(wheelRotation: Double, isHorizontal: Boolean, x: Int, y: Int) {

        if (!targetNode.lock.tryLock()) return

        Vector3f(0f,0f,1f).mul(
            wheelRotation.toFloat() * fpsSpeedSlow() * mouseSpeed() * if (camera.forward.z > 0) -1 else 1,
            scrollPosUpdater
        )
        targetNode.ifSpatial {
                position.add(scrollPosUpdater)
                needsUpdate = true
            }

        targetNode.lock.unlock()
    }

    //aux vars to prevent from re-creating them over and over
    private val scrollPosUpdater: Vector3f = Vector3f()
}
