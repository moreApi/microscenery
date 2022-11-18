package microscenery.example.microscope

import graphics.scenery.utils.extensions.times
import microscenery.DefaultScene
import microscenery.MicroscopeLayout
import microscenery.StageSpaceManager
import microscenery.hardware.DemoMicroscopeHardware
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

        val hw = DemoMicroscopeHardware(binning = 2)
        stageSpaceManager = StageSpaceManager(
            hw,
            scene,
            hub,
            addFocusFrame = true,
            scaleDownFactor = 100f,
            layout = MicroscopeLayout.Default(MicroscopeLayout.Axis.Z)
        )

        stageSpaceManager.stageRoot.spatial().scale *= Vector3f(1f, 1f, 1f)
        stageSpaceManager.focusFrame?.stageSteeringActive = true

        //val tfUI = TransferFunctionEditor(650, 550, stageSpaceManager)
        //tfUI.name = "Slices"

        thread {
            //Thread.sleep(5000)
            val db = DemoBehavior(
                hw.hardwareDimensions().stageMax.x,
                stageSpaceManager
            )
            //db.fixedStack()
            //Thread.sleep(2500)
            db.fixed()
            //db.fixed()
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
            stageSpaceManager.userInteraction(it, cam)
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            DemoHWScene().main()
        }
    }
}

