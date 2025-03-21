package microscenery.VRUI

import graphics.scenery.Scene
import graphics.scenery.attribute.spatial.HasSpatial
import graphics.scenery.attribute.spatial.Spatial
import graphics.scenery.controls.InputHandler
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackedDeviceType
import graphics.scenery.controls.TrackerRole
import graphics.scenery.controls.behaviours.Touch
import graphics.scenery.controls.behaviours.VRGrab
import graphics.scenery.controls.behaviours.VRTouch
import graphics.scenery.volumes.Volume
import microscenery.MicrosceneryHub
import microscenery.MicroscenerySettings
import microscenery.Settings
import microscenery.UI.StageSpaceUI
import microscenery.UI.UIModel
import microscenery.VRUI.behaviors.VR2HandSpatialManipulation
import microscenery.VRUI.behaviors.VRGrabTheWorldSelfMove
import microscenery.VRUI.behaviors.VRTeleport
import microscenery.VRUI.fromScenery.WheelMenu
import microscenery.wrapForAnalogInputIfNeeded
import org.scijava.ui.behaviour.DragBehaviour

class VRUIManager {

    companion object {
        fun initBehavior(
            scene: Scene,
            hmd: OpenVRHMD,
            inputHandler: InputHandler?,
            customActions: WheelMenu? = null,
            stageSpaceUI: StageSpaceUI? = null,
            alternativeTarget: Volume? = null,
            msHub: MicrosceneryHub
        ) {
            val uiModel = initUIModel(msHub, hmd)

            VRGrabTheWorldSelfMove.createAndSet(
                scene, hmd, listOf(OpenVRHMD.OpenVRButton.Down), listOf(TrackerRole.RightHand)
            )
            VRTeleport.createAndSet(
                scene, hmd, listOf(OpenVRHMD.OpenVRButton.Up),
                listOf(TrackerRole.RightHand)
            )

            inputHandler?.initStickMovement(hmd)

            val vr2HandSpatialManipulation =
                VR2HandSpatialManipulation.createAndSet(
                    hmd,
                    OpenVRHMD.OpenVRButton.Side,
                    scene,
                    stageSpaceManager = stageSpaceUI?.stageSpaceManager,
                    alternativeTarget = alternativeTarget?.spatial()
                )


            val pressButtons = listOf(
                OpenVRHMD.OpenVRButton.Trigger,
                OpenVRHMD.OpenVRButton.A,
                OpenVRHMD.OpenVRButton.Menu,
                OpenVRHMD.OpenVRButton.Side
            )
            InHandForwarder.createAndWrapVRPressWithInHandManagerBehavior(
                uiModel,
                scene,
                hmd,
                TrackerRole.LeftHand,
                pressButtons
            )
            InHandForwarder.createAndWrapVRPressWithInHandManagerBehavior(
                uiModel,
                scene,
                hmd,
                TrackerRole.RightHand,
                pressButtons
            )

            listOf(TrackerRole.RightHand, TrackerRole.LeftHand).forEach { side ->
                val grab = VRGrab.createAndSet(
                    scene,
                    hmd,
                    OpenVRHMD.OpenVRButton.Side,
                    side,
                    holdToDrag = false,
                    onGrab = { node, device -> (hmd as? OpenVRHMD)?.vibrate(device); Touch.unapplySelectionColor(node) })

                // use tool tip for grabbing when one is in hand
                var controllerCollider: Spatial? = null
                val handProperty = if (side == TrackerRole.LeftHand) UIModel::inLeftHand else UIModel::inRightHand
                uiModel.registerListener<VRHandTool>(handProperty) { old, new ->
                    if (grab.isDone) {
                        if (controllerCollider == null) controllerCollider = grab.get().controllerHitbox
                        if (new == null) {
                            grab.get().controllerHitbox = controllerCollider as Spatial
                        } else {
                            new.getTipCollider()?.let { grab.get().controllerHitbox = it }
                        }
                    }
                }
            }

            VRTouch.createAndSet(scene, hmd, listOf(TrackerRole.RightHand), false)
            VRTouch.createAndSet(scene, hmd, listOf(TrackerRole.LeftHand), false)

            // ----------------- Menus -----------------
            Toolbox(
                scene,
                hmd,
                TOOL_BOX_BUTTON,
                customActions?.let { WheelMenu(hmd, it.actions, true) },
                stageSpaceUI?.stageSpaceManager,
                uiModel
            )

            stageSpaceUI?.stageSpaceManager?.let {
                LeftHandMenu.init(it, vr2HandSpatialManipulation, scene, hmd)
            }
            /* TODO: build this in again?
                stageSpaceUI?.let { ssui ->
                    VRFastSelectionWheel.createAndSet(
                        scene, hmd, listOf(OpenVRHMD.OpenVRButton.Menu),
                        listOf(TrackerRole.LeftHand),
                        ssui.vrMenuActions()
                    )
                }
             */

            // ----------- volume timepoint scrolling -----------------
            val controllerSide = listOf(TrackerRole.RightHand)
            hmd.events.onDeviceConnect.add { _, device, _ ->
                if (device.type == TrackedDeviceType.Controller) {
                    device.model?.let { _ ->
                        if (controllerSide.contains(device.role)) {
                            run {
                                val name = "Volume scrolling prev"
                                val behavior = wrapForAnalogInputIfNeeded(
                                    scene,
                                    OpenVRHMD.OpenVRButton.Left,
                                    object : DragBehaviour {
                                        override fun init(x: Int, y: Int) {
                                            val vol = uiModel.selected as? Volume ?: alternativeTarget ?: return
                                            if (vol.currentTimepoint == 0) return

                                            vol.previousTimepoint()
                                            uiModel.updateSelected()
                                        }

                                        override fun drag(x: Int, y: Int) {}
                                        override fun end(x: Int, y: Int) {}
                                    })
                                hmd.addBehaviour(name, behavior)
                                hmd.addKeyBinding(name, device.role, OpenVRHMD.OpenVRButton.Left)
                            }
                            run {
                                val name = "Volume scrolling next"
                                val behavior = wrapForAnalogInputIfNeeded(
                                    scene,
                                    OpenVRHMD.OpenVRButton.Right,
                                    object : DragBehaviour {
                                        override fun init(x: Int, y: Int) {
                                            val vol = uiModel.selected as? Volume ?: alternativeTarget ?: return
                                            if (vol.currentTimepoint + 1 == vol.timepointCount) return

                                            vol.nextTimepoint()
                                            uiModel.updateSelected()
                                        }

                                        override fun drag(x: Int, y: Int) {}
                                        override fun end(x: Int, y: Int) {}
                                    })
                                hmd.addBehaviour(name, behavior)
                                hmd.addKeyBinding(name, device.role, OpenVRHMD.OpenVRButton.Right)
                            }
                        }
                    }
                }
            }
        }

        fun initUIModel(
            msHub: MicrosceneryHub,
            hmd: OpenVRHMD
        ): UIModel {
            val uiModel = msHub.getAttribute(UIModel::class.java)

            // register controller objects in ui model once they show up
            hmd.events.onDeviceConnect.add { _, device, _ ->
                if (device.type == TrackedDeviceType.Controller) {
                    device.model?.let {
                        when (device.role) {
                            TrackerRole.Invalid -> {}
                            TrackerRole.LeftHand -> uiModel.leftVRController = device
                            TrackerRole.RightHand -> uiModel.rightVRController = device
                        }
                    }
                }
            }
            return uiModel
        }

        private fun InputHandler.initStickMovement(hmd: OpenVRHMD) {
            val speed = MicroscenerySettings.setIfUnset(Settings.UI.FlySpeed, 0.5f)
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
                    val moveCommand = (b as? graphics.scenery.controls.behaviours.MovementCommand) ?: return
                    moveCommand.speed = speed
                    MicroscenerySettings.addUpdateRoutine(Settings.UI.FlySpeed) {
                        val speed2 = MicroscenerySettings.get(Settings.UI.FlySpeed, 0.5f)
                        moveCommand.speed = speed2
                    }
                }
            }
        }
    }
}


