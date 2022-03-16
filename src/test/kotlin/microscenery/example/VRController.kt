package microscenery.example

import graphics.scenery.Box
import graphics.scenery.Node
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackedDeviceType
import graphics.scenery.controls.TrackerRole
import graphics.scenery.controls.behaviours.Grabable
import graphics.scenery.controls.behaviours.Selectable
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import microscenery.DefaultVRScene
import microscenery.behaviors.VRGrabWithSelfMove
import org.joml.Vector3f
import kotlin.concurrent.thread

/**
 * Example for usage of VR controllers. Demonstrates the use of custom key bindings on the
 * HMD, the use of intersection testing with scene elements, and more advanced tools.
 *
 * Available Controls:
 * Side buttons alone:  Grab Object
 * Both side buttons together: Move to scale, after selection
 * Right Trigger:       Select to Scale
 * Left Trigger:        Select Party Cube
 * Left A Button:       Options Menu
 *
 * @author Ulrik Günther <hello@ulrik.is>
 */
class VRController : DefaultVRScene(
    VRController::class.java.simpleName
) {
    private lateinit var boxes: List<Node>
    private var leftControllerPushes = false

    override fun init() {

        prepareVRScene()

        boxes = (0..10).map {
            val obj = Box(Vector3f(0.1f, 0.1f, 0.1f))
            obj.spatial {
                position = Vector3f(-1.0f + (it + 1) * 0.2f, 1.0f, -0.5f)
            }
            obj.addAttribute(Grabable::class.java, Grabable())
            obj.addAttribute(Selectable::class.java, Selectable())
            obj
        }

        boxes.forEach { scene.addChild(it) }

        /** Box with rotated parent to debug grabbing
        val pivot = RichNode()
        pivot.spatial().rotation.rotateLocalY(Math.PI.toFloat())
        scene.addChild(pivot)

        val longBox = Box(Vector3f(0.1f, 0.2f, 0.1f))
        longBox.spatial {
        position = Vector3f(-0.5f, 1.0f, 0f)
        }
        longBox.addAttribute(Grabable::class.java, Grabable())
        pivot.addChild(longBox)
         */


        thread {
            while (!running) {
                Thread.sleep(200)
            }

            hmd.events.onDeviceConnect.add { hmd, device, timestamp ->
                if (device.type == TrackedDeviceType.Controller) {
                    logger.info("Got device ${device.name} at $timestamp")
                    device.model?.let { controller ->
                        // This update routine is called every time the position of the left controller
                        // updates, and checks for intersections with any of the boxes. If there is
                        // an intersection with a box, that box is slightly nudged in the direction
                        // of the controller's velocity.
                        if (device.role == TrackerRole.LeftHand) {
                            controller.update.add {
                                if (leftControllerPushes) {
                                    boxes.forEach { it.materialOrNull()?.diffuse = Vector3f(0.9f, 0.5f, 0.5f) }
                                    boxes.filter { box ->
                                        controller.children.first().spatialOrNull()?.intersects(box) ?: false
                                    }.forEach { box ->
                                        box.ifMaterial {
                                            diffuse = Vector3f(1.0f, 0.0f, 0.0f)
                                        }
                                        if (device.role == TrackerRole.LeftHand) {
                                            box.ifSpatial {
                                                position = (device.velocity ?: Vector3f(0.0f)) * 0.05f + position
                                            }
                                        }
                                        (hmd as? OpenVRHMD)?.vibrate(device)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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

        /** example of click input
        // Now we add another behaviour for toggling visibility of the boxes
        hmd.addBehaviour("toggle_boxes", ClickBehaviour { _, _ ->
        boxes.forEach { it.visible = !it.visible }
        logger.info("Boxes visible: ${boxes.first().visible}")
        })
        // ...and bind that to the A button of the left-hand controller.
        hmd.addKeyBinding("toggle_boxes", TrackerRole.RightHand, OpenVRHMD.OpenVRButton.A)
         */

        VRGrabWithSelfMove.createAndSet(
            scene,
            hmd,
            listOf(OpenVRHMD.OpenVRButton.Side),
            listOf(TrackerRole.LeftHand, TrackerRole.RightHand)
        )
// TODO adjust to new version
//        val selectionStorage =
//            VRSelect.createAndSetWithStorage(
//                scene,
//                hmd,
//                listOf(OpenVRHMD.OpenVRButton.Trigger),
//                listOf(TrackerRole.RightHand)
//            )
//        VRScale.createAndSet(hmd, OpenVRHMD.OpenVRButton.Side) {
//            selectionStorage.selected?.ifSpatial { scale *= Vector3f(it) }
//        }
//
//        VRSelect.createAndSetWithAction(scene,
//            hmd,
//            listOf(OpenVRHMD.OpenVRButton.Trigger),
//            listOf(TrackerRole.LeftHand),
//            { n ->
//                // this is just some action to show a successful selection.
//                // Party Cube!
//                val w = Wiggler(n.spatialOrNull()!!, 1.0f)
//                thread {
//                    sleep(2 * 1000)
//                    w.deativate()
//                }
//            })
//
//
//        VRSelectionWheel.createAndSet(scene, hmd, { hmd.getPosition() },
//            listOf(OpenVRHMD.OpenVRButton.A), listOf(TrackerRole.LeftHand),
//            listOf(
//                "Toggle Shell" to {
//                    hullbox.visible = !hullbox.visible
//                    logger.info("Hull visible: ${hullbox.visible}")
//                },
//                "Toggle Boxes" to {
//                    boxes.forEach { it.visible = !it.visible }
//                    logger.info("Boxes visible: ${boxes.first().visible}")
//                },
//                "test" to { print("test") },
//                "Toggle Push Left" to {
//                    leftControllerPushes = !leftControllerPushes
//                }
//            ))
    }


    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            VRController().main()
        }
    }
}