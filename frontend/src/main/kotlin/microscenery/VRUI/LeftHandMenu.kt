package microscenery.VRUI

import graphics.scenery.Scene
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackerRole
import graphics.scenery.volumes.Colormap
import microscenery.MicroscenerySettings
import microscenery.Settings
import microscenery.VRUI.Gui3D.*
import microscenery.VRUI.behaviors.VR2HandSpatialManipulation
import microscenery.stageSpace.StageSpaceManager
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.concurrent.CompletableFuture


object LeftHandMenu {
    fun init(
        stageSpaceManager: StageSpaceManager,
        scalingAndRotating: CompletableFuture<VR2HandSpatialManipulation>,
        scene: Scene,
        hmd: OpenVRHMD
    ) {
        val leftHandMenuTabs = mutableListOf<TabbedMenu.MenuTab>()


        val colorButtons = Colormap.list().map {
            Button(it) { MicroscenerySettings.set(Settings.StageSpace.ColorMap, it) }
        }
        val colorButtonRows = colorButtons.mapIndexedNotNull { index, _ ->
            if (index % 3 != 0) return@mapIndexedNotNull null
            val endSubList = minOf(index + 2, colorButtons.size)
            Row(*colorButtons.subList(index, endSubList).toTypedArray())
        }

        stageSpaceManager.sliceManager.transferFunctionManager.let { tf ->
            leftHandMenuTabs += TabbedMenu.MenuTab(
                "Img", Column(
                    *colorButtonRows.toTypedArray(),
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

                )
            )
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
                    Switch("hide plan", false, true, onChange = ablm::hidePlan),
                ), ablm::composeAblation, ablm::scrapAblation
            )
        }
        leftHandMenuTabs += TabbedMenu.MenuTab("Options", Column(
            Switch.forBoolSetting("fix Menu", Settings.VRUI.LeftHandMenuFixedPosition, true),
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
            scene, hmd, MENU_BUTTON,
            listOf(TrackerRole.LeftHand),
            ui = TabbedMenu(leftHandMenuTabs)
        )
    }
}