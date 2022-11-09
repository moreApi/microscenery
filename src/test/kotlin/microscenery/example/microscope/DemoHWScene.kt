package microscenery.example.microscope

import graphics.scenery.utils.extensions.times
import graphics.scenery.volumes.TransferFunctionEditor
import microscenery.DefaultScene
import microscenery.StageSpaceManager
import microscenery.hardware.DemoMicroscopeHardware
import org.joml.Vector3f
import kotlin.concurrent.thread


class DemoHWScene : DefaultScene() {
    init {
        logger.info("Starting demo hw scene")

//        MicroscenerySettings.set("Stage.minX", 40000f)
//        MicroscenerySettings.set("Stage.minY", 22000f)
//        MicroscenerySettings.set("Stage.minZ", -50f)
//        MicroscenerySettings.set("Stage.maxX", 44000f)
//        MicroscenerySettings.set("Stage.maxY", 26000f)
//        MicroscenerySettings.set("Stage.maxZ", 100f)

        val hw = DemoMicroscopeHardware()
        val stageSpaceManager = StageSpaceManager(hw, scene, hub, addFocusFrame = true)

        stageSpaceManager.stageRoot.spatial().scale *= Vector3f(1f, 1f, 2f)

        val tfUI = TransferFunctionEditor(650, 550, stageSpaceManager)
        tfUI.name = "Slices"


        thread {
            //Thread.sleep(5000)
            val db = DemoBehavior(
                hw.side.toFloat(),
                stageSpaceManager
            )
            //db.fixedStack()
            //Thread.sleep(2500)
            db.randomStatic()
        }
        thread {
            while (true) {
                Thread.sleep(200)
                scene to stageSpaceManager
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

