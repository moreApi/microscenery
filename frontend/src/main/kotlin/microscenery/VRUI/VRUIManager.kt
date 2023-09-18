package microscenery.VRUI

import graphics.scenery.Scene
import graphics.scenery.controls.InputHandler
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackedDeviceType
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
import microscenery.showMessage2
import microscenery.stageSpace.StageSpaceManager
import microscenery.wrapForAnalogInputIfNeeded
import org.joml.Quaternionf
import org.joml.Vector3f
import org.scijava.ui.behaviour.DragBehaviour
import java.util.concurrent.CompletableFuture

class VRUIManager {

    companion object {
        fun initBehavior(
            scene: Scene,
            hmd: OpenVRHMD,
            inputHandler: InputHandler?,
            customActions: WheelMenu? = null,
            stageSpaceUI: StageSpaceUI? = null,
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
            val touchRightHand = VRTouch.createAndSet(scene, hmd, listOf(TrackerRole.RightHand), false)
            VRTouch.createAndSet(scene, hmd, listOf(TrackerRole.LeftHand), false)
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
                listOf(MENU_BUTTON,OpenVRHMD.OpenVRButton.A),
                listOf(TrackerRole.RightHand),
                customActions?.let { WheelMenu(hmd, it.actions, true) },
                stageSpaceUI?.stageSpaceManager
            ) { touchRightHand.get()?.selected?.isEmpty() ?: true }

            stageSpaceUI?.stageSpaceManager?.let {
                leftHandMenu(it, vr2HandSpatialManipulation, scene, hmd)
            }
            /* TODO: build this in again
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
                    device.model?.let { controller ->
                        if (controllerSide.contains(device.role)) {
                            run{
                                val name = "Volume scrolling prev"
                                val behavior = wrapForAnalogInputIfNeeded( scene, OpenVRHMD.OpenVRButton.Left,object:DragBehaviour{
                                    override fun init(x: Int, y: Int) {
                                        stageSpaceUI?.stageSpaceManager?.sliceManager?.selectedStack?.volume?.let { vol ->
                                            val goto = vol.previousTimepoint()
                                            scene.findObserver()?.showMessage2("Timepoint $goto")
                                        }
                                    }
                                    override fun drag(x: Int, y: Int) {}
                                    override fun end(x: Int, y: Int) {}
                                })
                                hmd.addBehaviour(name, behavior)
                                hmd.addKeyBinding(name, device.role, OpenVRHMD.OpenVRButton.Left)
                            }
                            run{
                                val name = "Volume scrolling next"
                                val behavior = wrapForAnalogInputIfNeeded( scene, OpenVRHMD.OpenVRButton.Right,object:DragBehaviour{
                                    override fun init(x: Int, y: Int) {
                                        stageSpaceUI?.stageSpaceManager?.sliceManager?.selectedStack?.volume?.let { vol ->
                                            val goto = vol.nextTimepoint()
                                            scene.findObserver()?.showMessage2("Timepoint $goto")
                                        }
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

        private fun leftHandMenu(
            stageSpaceManager: StageSpaceManager,
            scalingAndRotating: CompletableFuture<VR2HandSpatialManipulation>,
            scene: Scene,
            hmd: OpenVRHMD
        ) {
            val leftHandMenuTabs = mutableListOf<TabbedMenu.MenuTab>()
            stageSpaceManager.sliceManager.transferFunctionManager.let { tf ->
                leftHandMenuTabs += TabbedMenu.MenuTab("Img", Column(
                    Row(TextBox("Display Range", height = 0.8f)),
                    ValueEdit(tf.minDisplayRange,
                        { tf.minDisplayRange += 10f;tf.minDisplayRange },
                        { tf.minDisplayRange -= 10f;tf.minDisplayRange },
                        { tf.minDisplayRange += 100f;tf.minDisplayRange },
                        { tf.minDisplayRange -= 100f;tf.minDisplayRange }),
                    ValueEdit(tf.maxDisplayRange,
                        { tf.maxDisplayRange += 10f;tf.maxDisplayRange },
                        { tf.maxDisplayRange -= 10f;tf.maxDisplayRange },
                        { tf.maxDisplayRange += 100f;tf.maxDisplayRange },
                        { tf.maxDisplayRange -= 100f;tf.maxDisplayRange }),
                    Row(Button("snap") { stageSpaceManager.snapSlice() })

                ))
            }
            val ablm = stageSpaceManager.ablationManager
            if (MicroscenerySettings.get(Settings.Ablation.Enabled, false)) {
                leftHandMenuTabs += TabbedMenu.MenuTab(
                    "Ablation", Column(
                        Row(Button("ablate", height = 1.3f) {
                            ablm.executeAblation()
                        }),
//                        Row(TextBox("laser power", height = 0.8f)),
//                        ValueEdit.forFloatSetting(Settings.Ablation.LaserPower, 0.1f),
//                        Row(TextBox("step size", height = 0.8f)),
//                        ValueEdit.forIntSetting(Settings.Ablation.StepSizeUm, 10),
                        Row(TextBox("repetitions", height = 0.8f)),
                        ValueEdit.forIntSetting(Settings.Ablation.Repetitions, plusPlusButtons = false),
                        Switch("hide plan",false, true, onChange = ablm::hidePlan),
                    ), ablm::composeAblation, ablm::scrapAblation
                )
            }
            leftHandMenuTabs += TabbedMenu.MenuTab("Options", Column(
                Switch.forBoolSetting("fix Menu",Settings.VRUI.LeftHandMenuFixedPosition, true),
                Switch("lock scaling", false, true)
                { scalingAndRotating.getNow(null)?.scaleLocked = it },
                Switch("lock rotation", false, true)
                { scalingAndRotating.getNow(null)?.rotationLocked = it },
                Button("reset") {
                    scene.activeObserver?.spatial {
                        position = Vector3f(0.0f, 0.0f, 5.0f)
                    }
                    stageSpaceManager.scaleAndRotationPivot.spatial {
                        rotation = Quaternionf()
                        scale = Vector3f(1f)
                        position = Vector3f()
                    }
                }
            ))
            VR3DGui.createAndSet(
                scene, hmd, listOf(OpenVRHMD.OpenVRButton.Menu, OpenVRHMD.OpenVRButton.A),
                listOf(TrackerRole.LeftHand),
                ui = TabbedMenu(leftHandMenuTabs)
            )
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

