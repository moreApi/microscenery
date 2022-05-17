package microscenery.VRUI

import graphics.scenery.Scene
import graphics.scenery.Sphere
import graphics.scenery.controls.InputHandler
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackedDeviceType
import graphics.scenery.controls.TrackerRole
import graphics.scenery.controls.behaviours.VRPress
import graphics.scenery.controls.behaviours.VRScale
import graphics.scenery.controls.behaviours.VRTouch
import graphics.scenery.utils.extensions.times
import graphics.scenery.volumes.Volume
import microscenery.VRUI.behaviors.VRGrabWithSelfMove
import microscenery.VRUI.behaviors.VRTeleport
import org.joml.Vector3f

class VRUIManager {
    companion object {
        fun initBehavior(scene: Scene, hmd: OpenVRHMD, inputHandler: InputHandler?, volume: Volume) {
            initControllerIndicator(hmd)

            VRGrabWithSelfMove.createAndSet(
                scene, hmd, listOf(OpenVRHMD.OpenVRButton.Side), listOf(TrackerRole.RightHand, TrackerRole.LeftHand)
            ) { VRTouch.unapplySelectionColor(it) }
            inputHandler?.initStickMovement(hmd)
            VRTeleport.createAndSet(scene, hmd, listOf(OpenVRHMD.OpenVRButton.Up,OpenVRHMD.OpenVRButton.Down), listOf(TrackerRole.LeftHand))

            VRScale.createAndSet(hmd, OpenVRHMD.OpenVRButton.Side) {
                volume.ifSpatial { scale *= Vector3f(it) }
            }

            VRTouch.createAndSet(scene, hmd, listOf(TrackerRole.RightHand, TrackerRole.LeftHand), false)
            VRPress.createAndSet(
                scene,
                hmd,
                listOf(OpenVRHMD.OpenVRButton.Trigger, OpenVRHMD.OpenVRButton.A),
                listOf(TrackerRole.RightHand, TrackerRole.LeftHand)
            )
            Toolbox(scene, hmd, listOf(OpenVRHMD.OpenVRButton.Menu), listOf(TrackerRole.RightHand), volume)
        }

        private fun InputHandler.initStickMovement(hmd: OpenVRHMD) {
            // We first grab the default movement actions from scenery's input handler,
            // and re-bind them on the right-hand controller's trackpad or joystick.
            hashMapOf(
                "move_forward" to OpenVRHMD.keyBinding(TrackerRole.RightHand, OpenVRHMD.OpenVRButton.Up),
                "move_back" to OpenVRHMD.keyBinding(TrackerRole.RightHand, OpenVRHMD.OpenVRButton.Down),
                "move_left" to OpenVRHMD.keyBinding(TrackerRole.RightHand, OpenVRHMD.OpenVRButton.Left),
                "move_right" to OpenVRHMD.keyBinding(TrackerRole.RightHand, OpenVRHMD.OpenVRButton.Right)
            ).forEach { (name, key) ->
                getBehaviour(name)?.let { b ->
                    hmd.addBehaviour(name, b)
                    hmd.addKeyBinding(name, key)
                }
            }
        }

        private fun initControllerIndicator(hmd: OpenVRHMD){
            hmd.events.onDeviceConnect.add { _, device, _ ->
                if (device.type == TrackedDeviceType.Controller) {
                    device.model?.let { controller ->
                        val indicator = Sphere(0.025f,10)
                        indicator.material().diffuse = Vector3f(1f)
                        controller.addChild(indicator)
                    }
                }
            }
        }
    }
}

