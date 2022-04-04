package microscenery.VRUI

import graphics.scenery.Box
import graphics.scenery.Node
import graphics.scenery.Scene
import graphics.scenery.attribute.spatial.Spatial
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackedDeviceType
import graphics.scenery.controls.TrackerRole
import graphics.scenery.controls.behaviours.*
import microscenery.DefaultVRScene
import microscenery.behaviors.VRGrabWithSelfMove
import org.joml.Vector3f
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future


class VRUIManager(scene: Scene) {

    val dummyTool = Box(Vector3f(0.1f, 0.2f, 0.07f)).let {
        scene.addChild(it)
        it.addVRToolFunctionality()
        it.visible = false
        it
    }


    fun initBehavior(scene: Scene, hmd: OpenVRHMD) {

        VRGrabWithSelfMove.createAndSet(
            scene,
            hmd,
            listOf(OpenVRHMD.OpenVRButton.Side),
            listOf(TrackerRole.RightHand, TrackerRole.LeftHand)
        )
        VRPress.createAndSet(
            scene,
            hmd,
            listOf(OpenVRHMD.OpenVRButton.Trigger),
            listOf(TrackerRole.RightHand, TrackerRole.LeftHand)
        ) {it,_ -> VRTouch.unapplySelectionColor(it) }

        VRSelectionWheel.createAndSet(
            scene,
            hmd,
            listOf(OpenVRHMD.OpenVRButton.Menu),
            listOf(TrackerRole.RightHand, TrackerRole.LeftHand),
            listOf("dummy" to { device ->
                dummyTool.visible = true
                dummyTool.spatial().position = device.worldPosition()
            })
        )
    }
}

fun Node.addVRToolFunctionality(
    onPickup: () -> Unit = {},
    whileHeld: () -> Unit = {},
    onDrop: () -> Unit = {},
    pressable: Pressable = SimplePressable()
) {
    this.addAttribute(Touchable::class.java, Touchable())
    this.addAttribute(Grabable::class.java, Grabable(onPickup, whileHeld, onDrop, false))
    this.addAttribute(Pressable::class.java, pressable)
}

class Demo : DefaultVRScene(VRUIManager::class.java.simpleName) {


    override fun init() {
        prepareVRScene()
    }

    override fun inputSetup() {
        super.inputSetup()

        // We first grab the default movement actions from scenery's input handler,
        // and re-bind them on the right-hand controller's trackpad or joystick.
        inputHandler?.let { handler ->
            hashMapOf(
                "move_forward" to OpenVRHMD.keyBinding(TrackerRole.RightHand, OpenVRHMD.OpenVRButton.Up),
                "move_back" to OpenVRHMD.keyBinding(TrackerRole.RightHand, OpenVRHMD.OpenVRButton.Down),
                "move_left" to OpenVRHMD.keyBinding(TrackerRole.RightHand, OpenVRHMD.OpenVRButton.Left),
                "move_right" to OpenVRHMD.keyBinding(TrackerRole.RightHand, OpenVRHMD.OpenVRButton.Right)
            ).forEach { (name, key) ->
                handler.getBehaviour(name)?.let { b ->
                    logger.info("Adding behaviour $name bound to $key to HMD")
                    hmd.addBehaviour(name, b)
                    hmd.addKeyBinding(name, key)
                }
            }
        }

        VRUIManager(scene).initBehavior(scene, hmd)

    }
}

fun main() {
    Demo().main()
}
