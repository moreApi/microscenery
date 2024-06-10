package microscenery.example.microscope

import graphics.scenery.Node
import graphics.scenery.utils.extensions.times
import microscenery.*
import microscenery.UI.StageSpaceUI
import microscenery.UI.UIModel
import microscenery.stageSpace.MicroscopeLayout
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector3f
import kotlin.concurrent.thread


class FileHWScene : DefaultScene(withSwingUI = false) {
    lateinit var stageSpaceManager: StageSpaceManager
    val msHub = MicrosceneryHub(hub)

    override fun init() {
        super.init()
        logger.info("Starting demo hw scene")
        cam.spatial().position = Vector3f(0f, 0f, 5f)


        MicroscenerySettings.set("Stage.precisionXY", 1f)
        MicroscenerySettings.set("Stage.precisionZ", 1f)

        val viewSettings = listOf(
            Settings.StageSpace.viewMode,
            Settings.StageSpace.HideFocusFrame,
            Settings.StageSpace.HideFocusTargetFrame,
            Settings.StageSpace.HideStageSpaceLabel
        )
        viewSettings.forEach { MicroscenerySettings.set(it, true) }

        val hw = FileMicroscopeHardware("""C:\Users\JanCasus\Downloads\LNDW_VR\emd_3743Stright.tif kept stack.tif""")
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

        StageSpaceUI(stageSpaceManager).stageUI(this, inputHandler, msHub)

        msHub.getAttribute(UIModel::class.java).changeEvents += {
            when (it.kProperty) {
                UIModel::selected -> println("${(it.new as Node).name} selected")
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            FileHWScene().main()
        }
    }
}


