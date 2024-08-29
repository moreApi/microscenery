package microscenery.scenes.microscope

import graphics.scenery.Node
import graphics.scenery.utils.extensions.times
import graphics.scenery.volumes.Colormap
import graphics.scenery.volumes.TransferFunction
import microscenery.*
import microscenery.UI.StageSpaceUI
import microscenery.UI.UIModel
import microscenery.VRUI.VRUIManager
import microscenery.stageSpace.MicroscopeLayout
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector3f
import kotlin.concurrent.thread


class FileHWScene : DefaultScene(withSwingUI = false, VR = true) {
    lateinit var stageSpaceManager: StageSpaceManager
    val msHub = MicrosceneryHub(hub)

    init {
        MicroscenerySettings.set("Stage.precisionXY", 1f)
        MicroscenerySettings.set("Stage.precisionZ", 1f)

        MicroscenerySettings.set(Settings.VRToolbox.PointAblationEnabled, true)
        MicroscenerySettings.set(Settings.StageSpace.ShowHullbox, true)

        val viewSettings = listOf(
            Settings.StageSpace.viewMode,
            Settings.StageSpace.HideFocusFrame,
            Settings.StageSpace.HideFocusTargetFrame,
            Settings.StageSpace.HideStageSpaceLabel
        )
        viewSettings.forEach { MicroscenerySettings.set(it, true) }
    }

    override fun init() {
        super.init()
        logger.info("Starting demo hw scene")
        cam.spatial().position = Vector3f(0f, 0f, 5f)

        val hw = FileMicroscopeHardware("""E:\volumes\spindle\NikonSD_100x_R1EmESC_01-1.tif""")
        stageSpaceManager = StageSpaceManager(
            hw,
            scene,
            msHub,
            layout = MicroscopeLayout.Default(MicroscopeLayout.Axis.Z)
//            layout = MicroscopeLayout.Scape(MicroscopeLayout.Axis.Y, 33f)
        )

        stageSpaceManager.stageRoot.spatial().scale *= Vector3f(1f, 1f, 1f)

        stageSpaceManager.sliceManager.transferFunctionManager.maxDisplayRange = 65000f

        //tfUI.name = "Slices"

        thread {
            Thread.sleep(2000)
            stageSpaceManager.stack(Vector3f(), Vector3f())
            Thread.sleep(1000)

            stageSpaceManager.sliceManager.transferFunctionManager.transferFunction =
                TransferFunction.ramp(0.08605619f, 0.5125469f, 1f)
            stageSpaceManager.sliceManager.transferFunctionManager.colormap = Colormap.get("plasma")
            stageSpaceManager.sliceManager.transferFunctionManager.maxDisplayRange = 3300.0f
            stageSpaceManager.sliceManager.transferFunctionManager.minDisplayRange = 1125.0f
        }
        thread {
            while (true) {
                Thread.sleep(200)
                scene to stageSpaceManager
            }
        }
    }

    override fun inputSetup() {
        super.inputSetup()

        if (!VR) {
            StageSpaceUI(stageSpaceManager).stageUI(this, inputHandler, msHub)

            msHub.getAttribute(UIModel::class.java).changeEvents += {
                when (it.kProperty) {
                    UIModel::selected -> println("${(it.new as Node).name} selected")
                }
            }
        } else {
            val ssUI = StageSpaceUI(stageSpaceManager)

            inputHandler?.let {
                ssUI.stageKeyUI(it, cam)
            }

            VRUIManager.initBehavior(
                scene, hmd, inputHandler,
                stageSpaceUI = ssUI, msHub = MicrosceneryHub(hub)
            )
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            FileHWScene().main()
        }
    }
}


