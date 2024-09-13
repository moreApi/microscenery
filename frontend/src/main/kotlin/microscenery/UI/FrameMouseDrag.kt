package microscenery.UI

import graphics.scenery.BoundingGrid
import graphics.scenery.Node
import graphics.scenery.attribute.spatial.HasSpatial
import graphics.scenery.utils.lazyLogger
import microscenery.UP
import org.joml.Vector3f
import org.scijava.ui.behaviour.DragBehaviour
import org.scijava.ui.behaviour.ScrollBehaviour

/**
 *
 * Liberatly adapted from [MouseDragPlane]
 * @author Jan Tiemann
 */
open class FrameMouseDrag(
    protected val targetNode: HasSpatial,
    protected val mouseSpeed: () -> Float = { 0.25f },
    protected val fpsSpeedSlow: () -> Float = { 0.05f },
    val name: String = "Frame Mouse Drag"
) : DragBehaviour, ScrollBehaviour{

    protected val logger by lazyLogger()

    private var lastX = 0
    private var lastY = 0


    /**
     * This function is called upon mouse down and initializes the camera control
     * with the current window size.
     *
     * x position in window
     * y position in window
     */
    override fun init(x: Int, y: Int) {
        lastX = x
        lastY = y
    }

    override fun drag(x: Int, y: Int) {

        if (!targetNode.lock.tryLock()) return

        targetNode.ifSpatial {
            Vector3f(1f,0f,0f).mul((x - lastX) * fpsSpeedSlow() * mouseSpeed(), dragPosUpdater)
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
            wheelRotation.toFloat() * fpsSpeedSlow() * mouseSpeed(),
            scrollPosUpdater
        )
        targetNode.ifSpatial {
                position.add(scrollPosUpdater)
                needsUpdate = true
            }

        targetNode.lock.unlock()
    }

    //aux vars to prevent from re-creating them over and over
    private val dragPosUpdater: Vector3f = Vector3f()
    private val scrollPosUpdater: Vector3f = Vector3f()
}
