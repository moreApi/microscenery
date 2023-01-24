package microscenery.example.microscope

import graphics.scenery.utils.extensions.times
import microscenery.DefaultScene
import microscenery.MicroscenerySettings
import microscenery.UI.StageSpaceUI.Companion.stageUserInteraction
import microscenery.hardware.DemoMicroscopeHardware
import microscenery.stageSpace.MicroscopeLayout
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector3f
import kotlin.concurrent.thread


class DemoHWScene : DefaultScene() {
    lateinit var stageSpaceManager: StageSpaceManager

    override fun init() {
        super.init()
        logger.info("Starting demo hw scene")
        cam.spatial().position = Vector3f(0f, 0f, 5f)

//        MicroscenerySettings.set("Stage.minX", 40000f)
//        MicroscenerySettings.set("Stage.minY", 22000f)
//        MicroscenerySettings.set("Stage.minZ", -50f)
//        MicroscenerySettings.set("Stage.maxX", 44000f)
//        MicroscenerySettings.set("Stage.maxY", 26000f)
//        MicroscenerySettings.set("Stage.maxZ", 100f)
        MicroscenerySettings.set("Stage.precisionXY", 10f)
        MicroscenerySettings.set("Stage.precisionZ", 10f)


        val hw = DemoMicroscopeHardware(binning = 1)
        stageSpaceManager = StageSpaceManager(
            hw,
            scene,
            hub,
            addFocusFrame = true,
            scaleDownFactor = 100f,
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
//            db.randomStatic()
            //db.fixed()
            //db.fixedStack()

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

        inputHandler?.let {
            stageUserInteraction(stageSpaceManager, it, cam)
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            DemoHWScene().main()
        }
    }
}


