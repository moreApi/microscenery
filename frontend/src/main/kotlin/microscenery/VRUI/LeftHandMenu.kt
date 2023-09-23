package microscenery.VRUI

import graphics.scenery.Scene
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackerRole
import graphics.scenery.volumes.Colormap
import microscenery.MicroscenerySettings
import microscenery.Settings
import microscenery.VRUI.Gui3D.*
import microscenery.VRUI.behaviors.VR2HandSpatialManipulation
import microscenery.getVector3
import microscenery.setVector3f
import microscenery.stageSpace.StageSpaceManager
import org.joml.Quaternionf
import org.joml.Vector3f
import java.lang.Float.max
import java.util.concurrent.CompletableFuture
import kotlin.math.roundToInt


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
            val endSubList = minOf(index + 2, colorButtons.size-1)
            Row(*colorButtons.subList(index, endSubList+1).toTypedArray())
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
                    Row(TextBox("dwell time", height = 0.8f)),
                    ValueEdit.forIntSetting(Settings.Ablation.Repetitions, factor = 1,min = 0, plusPlusButtons = false){"${it}us"},
                    Row(TextBox("step size", height = 0.8f)),
                    createStepSizeEdit(),
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

    private fun createStepSizeEdit(): Gui3DElement {
        val factor = 0.05f
        val setting = Settings.Ablation.PrecisionUM
        val min = 0.01f

        fun getFloatStepSize(): Float {
            val vec = MicroscenerySettings.getVector3(setting) ?: Vector3f(2f)
            return vec.x
        }
        fun changeAndSave(value: Float, change: Float): Float {
            val t = max(((value + change * factor) * 100).roundToInt() * 0.01f,min)
            MicroscenerySettings.setVector3f(setting, Vector3f(t))
            return t
        }
        return ValueEdit(getFloatStepSize(),
            {changeAndSave(it,1f)},
            {changeAndSave(it,-1f)},
            {changeAndSave(it,10f)},
            {changeAndSave(it,-10f)},
            { "${"%.2f".format(it)}um"}
        )
    }
}