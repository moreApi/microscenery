package microscenery.example.microscope

import graphics.scenery.Node
import graphics.scenery.utils.extensions.times
import microscenery.DefaultScene
import microscenery.DemoMicroscopeHardware
import microscenery.MicrosceneryHub
import microscenery.MicroscenerySettings
import microscenery.UI.DesktopUI
import microscenery.UI.StageSpaceUI
import microscenery.UI.UIModel
import microscenery.stageSpace.MicroscopeLayout
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector3f
import kotlin.concurrent.thread


class DemoHWScene : DefaultScene(withSwingUI = true) {
    lateinit var stageSpaceManager: StageSpaceManager
    val msHub = MicrosceneryHub(hub)

    override fun init() {
        super.init()
        logger.info("Starting demo hw scene")
        cam.spatial().position = Vector3f(0f, 0f, 5f)


        MicroscenerySettings.set("Stage.precisionXY", 1f)
        MicroscenerySettings.set("Stage.precisionZ", 1f)


        val hw = DemoMicroscopeHardware(binning = 1)
        stageSpaceManager = StageSpaceManager(
            hw,
            scene,
            msHub,
            layout = MicroscopeLayout.Default(MicroscopeLayout.Axis.Z)
//            layout = MicroscopeLayout.Scape(MicroscopeLayout.Axis.Y, 33f)
        )

        stageSpaceManager.stageRoot.spatial().scale *= Vector3f(1f, 1f, 1f)

        //tfUI.name = "Slices"

        thread {
            //Thread.sleep(5000)
            @Suppress("UNUSED_VARIABLE") val db = DemoBehavior(
                hw.hardwareDimensions().stageMax.x,
                stageSpaceManager
            )
            //db.fixedStack(Vector3f(100f,100f,000f), Vector3f(100f, 100f,200f))
            //Thread.sleep(2500)
            //db.randomStatic()
            //db.fixed()
            db.fixedStack()

            //stageSpaceManager.sampleStageSpace(Vector3f(25f), Vector3f(175f), Vector3f(30f, 30f, 50f))
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

        StageSpaceUI(stageSpaceManager).stageUI(this, inputHandler)
        DesktopUI.initMouseSelection(inputHandler,msHub)

        msHub.getAttribute(UIModel::class.java).changeEvents += {
            when(it.kProperty){
                UIModel::selected -> println("${(it.new as Node).name} selected")
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            DemoHWScene().main()
        }
    }
}


