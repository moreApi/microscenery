package microscenery.VRUI

import graphics.scenery.Node
import graphics.scenery.Scene
import graphics.scenery.controls.InputHandler
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackerRole
import graphics.scenery.controls.behaviours.*
import microscenery.MicroscenerySettings
import microscenery.Settings
import microscenery.UI.StageSpaceUI
import microscenery.VRUI.Gui3D.*
import microscenery.VRUI.behaviors.VR2HandSpatialManipulation
import microscenery.VRUI.behaviors.VRGrabTheWorldSelfMove
import microscenery.VRUI.behaviors.VRTeleport
import microscenery.VRUI.fromScenery.WheelMenu

class VRUIManager {

    companion object {
        fun initBehavior(
            scene: Scene,
            hmd: OpenVRHMD,
            inputHandler: InputHandler?,
            customActions: WheelMenu? = null,
            stageSpaceUI: StageSpaceUI? = null,
            target: () -> Node? = {null}, //TODO unused
        ) {

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
                    rotationLocked = false,
                    stageSpaceManager = stageSpaceUI?.stageSpaceManager
                )

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
                onGrab = { node, device -> (hmd as? OpenVRHMD)?.vibrate(device); Touch.unapplySelectionColor(node) })
            val touch = VRTouch.createAndSet(scene, hmd, listOf(TrackerRole.RightHand, TrackerRole.LeftHand), false)
            VRPress.createAndSet(
                scene,
                hmd,
                listOf(OpenVRHMD.OpenVRButton.Trigger, OpenVRHMD.OpenVRButton.A, OpenVRHMD.OpenVRButton.Menu),
                listOf(TrackerRole.RightHand, TrackerRole.LeftHand)
            )
            Toolbox(
                scene,
                hmd,
                listOf(MENU_BUTTON),
                listOf(TrackerRole.RightHand),
                customActionsPlusScaleSwitch,
                stageSpaceUI?.stageSpaceManager
            ) { touch.get()?.selected?.isEmpty() ?: true }

            if (MicroscenerySettings.get(Settings.Ablation.Enabled, false)){
//                VRFastSelectionWheel.createAndSet(scene,hmd, listOf(OpenVRHMD.OpenVRButton.Menu),
//                    listOf(TrackerRole.LeftHand),
//                    listOf(
//                    "plan ablation" to { stageSpaceUI?.stageSpaceManager?.ablationManager?.composeAblation()},
//                    "remove plan" to { stageSpaceUI?.stageSpaceManager?.ablationManager?.scrapAblation() },
//                    "ablate path" to { stageSpaceUI?.stageSpaceManager?.ablationManager?.executeAblation() },
//                    )
//                )
                val tf = stageSpaceUI?.stageSpaceManager?.sliceManager?.transferFunctionManager ?: return
                VR3DGui.createAndSet(scene,hmd, listOf(OpenVRHMD.OpenVRButton.Menu),
                    listOf(TrackerRole.LeftHand),
                    WheelMenu.TrackingMode.LIVE,
                    ui = TabbedMenu(
                        "Displ Rng" to Column(
                            Row(TextBox("Display Range", height = 0.8f)),
                            ValueEdit(tf.maxDisplayRange,{tf.maxDisplayRange+=10f;tf.maxDisplayRange},{tf.maxDisplayRange-=10f;tf.maxDisplayRange},{tf.maxDisplayRange+=100f;tf.maxDisplayRange},{tf.maxDisplayRange-=100f;tf.maxDisplayRange}),
                            ValueEdit(tf.minDisplayRange,{tf.minDisplayRange+=10f;tf.minDisplayRange},{tf.minDisplayRange-=10f;tf.minDisplayRange},{tf.minDisplayRange+=100f;tf.minDisplayRange},{tf.minDisplayRange-=100f;tf.minDisplayRange}),
                        ),
                        "Ablation" to Column(
                            Row(TextBox("lasor bower", height = 0.8f)),
                            ValueEdit(0,{it+1},{it-1},{it+10},{it-10}),
                            Row(TextBox("step size", height = 0.8f)),
                            ValueEdit(500,{it+10},{it-10},{it+100},{it-100}),
                            Row(TextBox("repetitions", height = 0.8f)),
                            ValueEdit(1,{it+1},{it-1},{it+10},{it-10}),
                            Row(Button("ablate"){
                                println(" ablating!!!!")
                            })
                        ),
                    )
                )
            } else {
                stageSpaceUI?.let { ssui ->
                    VRFastSelectionWheel.createAndSet(
                        scene, hmd, listOf(OpenVRHMD.OpenVRButton.Menu),
                        listOf(TrackerRole.LeftHand),
                        ssui.vrMenuActions()
                    )
                }
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
    }
}

