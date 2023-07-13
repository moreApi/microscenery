package microscenery.UI

import graphics.scenery.Camera
import graphics.scenery.Node
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import org.joml.Vector3f
import org.scijava.ui.behaviour.ClickBehaviour
import kotlin.reflect.KProperty

/**
 * Movement Command class. Moves a node in the given direction.
 *
 *
 * @author Ulrik Günther <hello@ulrik.is>
 * @author Jan Tiemann
 * @property[direction] The direction of movement as string. Can be forward/back/left/right/up/down.
 * @property[n] The [Node] this behaviour affects.
 */
open class MovementCommand(
    private val direction: String,
    private var n: () -> Node?,
    val cam: Camera,
    var speed: Float = 0.5f
) : ClickBehaviour {
    private val node: Node? by NodeDelegate()

    protected inner class NodeDelegate {
        /** Returns the [graphics.scenery.Node] resulting from the evaluation of [n] */
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Node? {
            return n.invoke()
        }

        /** Setting the value is not supported */
        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Node?) {
            throw UnsupportedOperationException()
        }
    }

    /**
     * This function is triggered upon arrival of a click event that concerns
     * this behaviour. The camera is then moved in the corresponding direction.
     * this behaviour. The camera is then moved in the corresponding direction.
     */
    @Synchronized
    override fun click(x: Int, y: Int) {
        // see if the node is a camera, if not, try to find the active observer, and return
        // if that could not be found as well
        val deltaT = cam.deltaT
        val forward = Vector3f(0f, 0f, -1f)
        val right = Vector3f(1f, 0f, 0f)
        val up = Vector3f(0f, 1f, 0f)

        node?.let { node ->
            if (node.lock.tryLock() != false) {
                node.ifSpatial {
                    when (direction) {
                        "forward" -> position += forward * speed * deltaT * 1f / worldScale().z
                        "back" -> position -= forward * speed * deltaT * 1f / worldScale().z
                        "left" -> position -= right * speed * deltaT * 1f / worldScale().x
                        "right" -> position += right * speed * deltaT * 1f / worldScale().x
                        "up" -> position += up * speed * deltaT * 1f / worldScale().y
                        "down" -> position -= up * speed * deltaT * 1f / worldScale().y
                    }
                }

                node.lock.unlock()
            }
        }
    }
}
