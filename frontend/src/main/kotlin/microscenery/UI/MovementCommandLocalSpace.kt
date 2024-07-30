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
 * @author Ulrik Günther <hello@ulrik.is>
 * @author Jan Tiemann
 * @property[direction] The direction of movement as string. Can be forward/back/left/right/up/down.
 * @property[n] The [Node] this behaviour affects.
 */
open class MovementCommandLocalSpace(
    private val direction: String,
    private var n: () -> Node?,
    val cam: Camera,
    var speed: () -> Float = {1f}
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

    @Synchronized
    override fun click(x: Int, y: Int) {
        val forward = Vector3f(0f, 0f, -1f)
        val right = Vector3f(1f, 0f, 0f)
        val up = Vector3f(0f, 1f, 0f)


        node?.let { node ->
            if (node.lock.tryLock() != false) {
                node.ifSpatial {
                    when (direction) {
                        "forward" -> position += forward * speed()
                        "back" -> position -= forward * speed()
                        "left" -> position -= right * speed()
                        "right" -> position += right * speed()
                        "up" -> position += up * speed()
                        "down" -> position -= up * speed()
                    }
                }

                node.lock.unlock()
            }
        }
    }
}
