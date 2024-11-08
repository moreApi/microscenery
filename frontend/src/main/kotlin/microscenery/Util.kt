package microscenery

import graphics.scenery.Camera
import graphics.scenery.Node
import graphics.scenery.RichNode
import graphics.scenery.Scene
import graphics.scenery.attribute.material.HasMaterial
import graphics.scenery.controls.InputHandler
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.behaviours.Touchable
import microscenery.VRUI.Gui3D.Column
import microscenery.VRUI.Gui3D.Row
import microscenery.VRUI.Gui3D.TextBox
import microscenery.VRUI.behaviors.AnalogInputWrapper
import org.joml.Vector3f
import org.scijava.ui.behaviour.Behaviour
import org.scijava.ui.behaviour.DragBehaviour
import kotlin.concurrent.thread
import kotlin.random.Random


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
fun Node.detach() {
    this.parent?.removeChild(this)
}

fun HasMaterial.changeColorWithTouchable(newColor: Vector3f) {
    val touch = this.getAttributeOrNull(Touchable::class.java)
    if (touch?.originalDiffuse != null) {
        // this might screw with [VRTouch]s coloring
        touch.originalDiffuse = newColor
    } else {
        this.material().diffuse = newColor
    }
}

/**
 * Shows a [message] to the user, at a distance of [distance] meters.
 * The message can by styled by [size] (in meters).
 *
 * It will be shown for [duration] milliseconds, with a default of 3000.
 */
@JvmOverloads
fun Camera.showMessage2(message: String, distance: Float = 0.75f, size: Float = 0.05f, duration: Int = 3000) {
    val tb = TextBox(message)
    tb.spatial {
        scale = Vector3f(size, size, size)
        position = Vector3f(0.0f, 0.0f, -1.0f * distance)
    }

    @Suppress("UNCHECKED_CAST")
    val messages = metadata.getOrPut("messages", { mutableListOf<Node>() }) as? MutableList<Node>?
    messages?.forEach { this.removeChild(it) }
    messages?.clear()

    messages?.add(tb)
    this.addChild(tb)

    thread {
        Thread.sleep(duration.toLong())

        this.removeChild(tb)
        messages?.remove(tb)
    }
}


/**
 * Shows a [message] to the user, at a distance of [distance] meters.
 * The message can by styled by [size] (in meters).
 *
 * It will be shown for [duration] milliseconds, with a default of 3000.
 */
@JvmOverloads
fun Camera.showMessage2(message: List<String>, distance: Float = 0.75f, size: Float = 0.05f, duration: Int = 3000) {
    val tb = RichNode()

    tb.addChild(
        Column(
            *message.map {
                Row(TextBox(it))
            }.toTypedArray(),
            middleAlign = true,
            invertedYOrder = true
        )
    )
    tb.spatial {
        scale = Vector3f(size, size, size)
        position = Vector3f(0.0f, 0.0f, -1.0f * distance)
    }

    @Suppress("UNCHECKED_CAST")
    val messages = metadata.getOrPut("messages", { mutableListOf<Node>() }) as? MutableList<Node>?
    messages?.forEach { this.removeChild(it) }
    messages?.clear()

    messages?.add(tb)
    this.addChild(tb)

    thread {
        Thread.sleep(duration.toLong())

        this.removeChild(tb)
        messages?.remove(tb)
    }
}

/**
 * @return a list of nodes that return true for [func]
 */
fun Scene.discover(func: (Node) -> Boolean): ArrayList<Node> = this.discover(this, func, false)

/**
 * Depth first tree walk.
 */
fun <T> Node.treeWalk(func: (Node) -> T): List<T> = listOf(func(this)) + children.flatMap { it.treeWalk(func) }

fun InputHandler.addBehaviorBinding(behavior: Behaviour, vararg key: String, name: String? = null) {
    val behaviourName = name ?: (Random.nextInt().toString() + Random.nextInt().toString())
    this.addBehaviour(behaviourName, behavior)
    key.forEach {
        this.addKeyBinding(behaviourName, it)
    }
}

fun Random.nextVector3f(): Vector3f {
    return Vector3f(this.nextFloat(), this.nextFloat(), this.nextFloat())
}