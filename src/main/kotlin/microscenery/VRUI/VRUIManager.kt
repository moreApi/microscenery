package microscenery.VRUI

import graphics.scenery.Node
import graphics.scenery.Scene
import graphics.scenery.Sphere
import graphics.scenery.controls.InputHandler
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackedDeviceType
import graphics.scenery.controls.TrackerRole
import graphics.scenery.controls.behaviours.*
import microscenery.UI.StageSpaceUI
import microscenery.VRUI.behaviors.VR2HandSpatialManipulation
import microscenery.VRUI.behaviors.VRGrabTheWorldSelfMove
import microscenery.VRUI.behaviors.VRTeleport
import org.joml.Vector3f

class VRUIManager {

    companion object {
        fun initBehavior(
            scene: Scene,
            hmd: OpenVRHMD,
            inputHandler: InputHandler?,
            customActions: WheelMenu? = null,
            stageSpaceUI: StageSpaceUI? = null,
            target: () -> Node?,
        ) {
            initControllerIndicator(hmd)

            VRGrabTheWorldSelfMove.createAndSet(
                scene, hmd, listOf(OpenVRHMD.OpenVRButton.Down), listOf(TrackerRole.RightHand)
            )
            VRTeleport.createAndSet(
                scene, hmd, listOf(OpenVRHMD.OpenVRButton.Up),
                listOf(TrackerRole.RightHand)
            )

            inputHandler?.initStickMovement(hmd)

            val vr2HandSpatialManipulation =
                VR2HandSpatialManipulation.createAndSet(hmd, OpenVRHMD.OpenVRButton.Side, scene, rotationLocked = true){target()?.spatialOrNull()}
            val scalingLockToggle = Switch(
                "lock scaling", false
            ) { vr2HandSpatialManipulation.getNow(null)?.scaleLocked = it }
            val customActionsPlusScaleSwitch =
                WheelMenu(hmd, (customActions?.actions ?: emptyList()) + scalingLockToggle, true)

            VRGrab.createAndSet(
                scene,
                hmd,
                listOf(OpenVRHMD.OpenVRButton.Side),
                listOf(TrackerRole.RightHand, TrackerRole.LeftHand),
                holdToDrag = false,
                onGrab = { node, device -> (hmd as? OpenVRHMD)?.vibrate(device); VRTouch.unapplySelectionColor(node) })
            VRTouch.createAndSet(scene, hmd, listOf(TrackerRole.RightHand, TrackerRole.LeftHand), false)
            VRPress.createAndSet(
                scene,
                hmd,
                listOf(OpenVRHMD.OpenVRButton.Trigger, OpenVRHMD.OpenVRButton.A),
                listOf(TrackerRole.RightHand, TrackerRole.LeftHand)
            )
            Toolbox(
                scene,
                hmd,
                listOf(OpenVRHMD.OpenVRButton.Menu),
                listOf(TrackerRole.RightHand),
                customActionsPlusScaleSwitch,
                target
            )
            stageSpaceUI?.let { ssui ->
                VRSelectionWheel.createAndSet(
                    scene, hmd, listOf(OpenVRHMD.OpenVRButton.Menu),
                    listOf(TrackerRole.LeftHand),
                    ssui.vrMenuActions()
                )
            }
        }

        private fun InputHandler.initStickMovement(hmd: OpenVRHMD) {
            // We first grab the default movement actions from scenery's input handler,
            // and re-bind them on the right-hand controller's trackpad or joystick.
            hashMapOf(
                "move_forward" to OpenVRHMD.keyBinding(TrackerRole.LeftHand, OpenVRHMD.OpenVRButton.Up),
                "move_back" to OpenVRHMD.keyBinding(TrackerRole.LeftHand, OpenVRHMD.OpenVRButton.Down),
                "move_left" to OpenVRHMD.keyBinding(TrackerRole.LeftHand, OpenVRHMD.OpenVRButton.Left),
                "move_right" to OpenVRHMD.keyBinding(TrackerRole.LeftHand, OpenVRHMD.OpenVRButton.Right)
            ).forEach { (name, key) ->
                getBehaviour(name)?.let { b ->
                    hmd.addBehaviour(name, b)
                    hmd.addKeyBinding(name, key)
                }
            }
        }

        /**
         * Create aim balls on top of controllers
         */
        private fun initControllerIndicator(hmd: OpenVRHMD) {
            hmd.events.onDeviceConnect.add { _, device, _ ->
                if (device.type == TrackedDeviceType.Controller) {
                    device.model?.let { controller ->
                        val indicator = Sphere(0.025f, 10)
                        indicator.material().diffuse = Vector3f(1f)
                        controller.addChild(indicator)
                    }
                }
            }
        }
    }
}

