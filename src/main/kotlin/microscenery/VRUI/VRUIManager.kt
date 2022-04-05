package microscenery.VRUI

import graphics.scenery.Scene
import graphics.scenery.controls.InputHandler
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackerRole
import graphics.scenery.controls.behaviours.VRPress
import graphics.scenery.controls.behaviours.VRTouch
import microscenery.behaviors.VRGrabWithSelfMove
import microscenery.behaviors.VRTeleport


class VRUIManager {


    companion object {

        fun initBehavior(scene: Scene, hmd: OpenVRHMD, inputHandler: InputHandler?) {

            VRGrabWithSelfMove.createAndSet(
                scene,
                hmd,
                listOf(OpenVRHMD.OpenVRButton.Side),
                listOf(TrackerRole.RightHand, TrackerRole.LeftHand)
            ) { VRTouch.unapplySelectionColor(it) }
            inputHandler?.let { initStickMovement(it, hmd) }
            VRTeleport.createAndSet(scene, hmd, listOf(OpenVRHMD.OpenVRButton.Menu), listOf(TrackerRole.LeftHand))


            VRTouch.createAndSet(scene, hmd, listOf(TrackerRole.RightHand, TrackerRole.LeftHand), false)
            VRPress.createAndSet(
                scene,
                hmd,
                listOf(OpenVRHMD.OpenVRButton.Trigger, OpenVRHMD.OpenVRButton.A),
                listOf(TrackerRole.RightHand, TrackerRole.LeftHand)
            )
            Toolbox(scene, hmd, listOf(OpenVRHMD.OpenVRButton.Menu), listOf(TrackerRole.RightHand))
        }

        private fun initStickMovement(handler: InputHandler, hmd: OpenVRHMD) {
            // We first grab the default movement actions from scenery's input handler,
            // and re-bind them on the right-hand controller's trackpad or joystick.
            hashMapOf(
                "move_forward" to OpenVRHMD.keyBinding(TrackerRole.RightHand, OpenVRHMD.OpenVRButton.Up),
                "move_back" to OpenVRHMD.keyBinding(TrackerRole.RightHand, OpenVRHMD.OpenVRButton.Down),
                "move_left" to OpenVRHMD.keyBinding(TrackerRole.RightHand, OpenVRHMD.OpenVRButton.Left),
                "move_right" to OpenVRHMD.keyBinding(TrackerRole.RightHand, OpenVRHMD.OpenVRButton.Right)
            ).forEach { (name, key) ->
                handler.getBehaviour(name)?.let { b ->
                    hmd.addBehaviour(name, b)
                    hmd.addKeyBinding(name, key)
                }

            }
        }
    }
}

