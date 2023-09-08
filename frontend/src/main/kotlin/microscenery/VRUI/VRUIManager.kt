package microscenery.VRUI

import graphics.scenery.Node
import graphics.scenery.Scene
import graphics.scenery.controls.InputHandler
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackerRole
import graphics.scenery.controls.behaviours.Touch
import graphics.scenery.controls.behaviours.VRGrab
import graphics.scenery.controls.behaviours.VRPress
import graphics.scenery.controls.behaviours.VRTouch
import microscenery.MicroscenerySettings
import microscenery.Settings
import microscenery.UI.StageSpaceUI
import microscenery.VRUI.Gui3D.*
import microscenery.VRUI.behaviors.VR2HandSpatialManipulation
import microscenery.VRUI.behaviors.VRGrabTheWorldSelfMove
import microscenery.VRUI.behaviors.VRTeleport
import microscenery.VRUI.fromScenery.WheelMenu
import org.joml.Quaternionf
import org.joml.Vector3f

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
                    stageSpaceManager = stageSpaceUI?.stageSpaceManager
                )
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


            // ----------------- Menus -----------------
            Toolbox(
                scene,
                hmd,
                listOf(MENU_BUTTON),
                listOf(TrackerRole.RightHand),
                customActions?.let { WheelMenu(hmd, it.actions, true) },
                stageSpaceUI?.stageSpaceManager
            ) { touch.get()?.selected?.isEmpty() ?: true }


            val leftHandMenu = mutableListOf<TabbedMenu.MenuTab>()
            stageSpaceUI?.stageSpaceManager?.sliceManager?.transferFunctionManager?.let { tf ->
                leftHandMenu += TabbedMenu.MenuTab("Displ Rng", Column(
                    Row(TextBox("Display Range", height = 0.8f)),
                    ValueEdit(tf.minDisplayRange,{tf.minDisplayRange+=10f;tf.minDisplayRange},{tf.minDisplayRange-=10f;tf.minDisplayRange},{tf.minDisplayRange+=100f;tf.minDisplayRange},{tf.minDisplayRange-=100f;tf.minDisplayRange}),
                    ValueEdit(tf.maxDisplayRange,{tf.maxDisplayRange+=10f;tf.maxDisplayRange},{tf.maxDisplayRange-=10f;tf.maxDisplayRange},{tf.maxDisplayRange+=100f;tf.maxDisplayRange},{tf.maxDisplayRange-=100f;tf.maxDisplayRange}),
                )
                )
            }
            val ablm = stageSpaceUI?.stageSpaceManager?.ablationManager
            if (MicroscenerySettings.get(Settings.Ablation.Enabled, false) && ablm != null){
                leftHandMenu +=TabbedMenu.MenuTab(
                    "Ablation" , Column(
                        Row(TextBox("laser power", height = 0.8f)),
                        ValueEdit.forFloatSetting(Settings.Ablation.LaserPower,0.1f),
                        Row(TextBox("step size", height = 0.8f)),
                        ValueEdit.forFloatSetting(Settings.Ablation.StepSizeUm,10f),
                        Row(TextBox("repetitions", height = 0.8f)),
                        ValueEdit.forIntSetting(Settings.Ablation.Repetitions, plusPlusButtons = false),
                        Row(Button("ablate"){
                            ablm.executeAblation()
                        })
                    ),ablm::composeAblation, ablm::scrapAblation
                )
            }
            leftHandMenu += TabbedMenu.MenuTab("Options", Column(
                Switch("lock scaling", false, true)
                { vr2HandSpatialManipulation.getNow(null)?.scaleLocked = it },
                Switch("lock rotation", false, true)
                { vr2HandSpatialManipulation.getNow(null)?.rotationLocked = it },
                Button("reset") {
                    scene.activeObserver?.spatial {
                        position = Vector3f(0.0f, 0.0f, 5.0f)
                    }
                    stageSpaceUI?.stageSpaceManager?.scaleAndRotationPivot?.spatial {
                        rotation = Quaternionf()
                        scale = Vector3f(1f)
                        position = Vector3f()
                    }
                }
            ))
            VR3DGui.createAndSet(scene,hmd, listOf(OpenVRHMD.OpenVRButton.Menu),
                listOf(TrackerRole.LeftHand),
                WheelMenu.TrackingMode.LIVE,
                ui = TabbedMenu(leftHandMenu)
            )
            /* TODO: build this in again
                stageSpaceUI?.let { ssui ->
                    VRFastSelectionWheel.createAndSet(
                        scene, hmd, listOf(OpenVRHMD.OpenVRButton.Menu),
                        listOf(TrackerRole.LeftHand),
                        ssui.vrMenuActions()
                    )
                }
             */
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

