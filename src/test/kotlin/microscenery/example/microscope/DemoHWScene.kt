package microscenery.example.microscope

import graphics.scenery.Box
import graphics.scenery.attribute.material.Material
import graphics.scenery.utils.extensions.times
import microscenery.DefaultScene
import microscenery.StageSpaceManager
import microscenery.hardware.DemoMicroscopeHardware
import org.joml.Vector3f
import kotlin.concurrent.thread

class DemoHWScene : DefaultScene() {
    init {
        logger.info("Starting demo hw scene")

        val hw = DemoMicroscopeHardware()
        val stageSpaceManager = StageSpaceManager(hw, scene, hub, addFocusFrame = true)

        stageSpaceManager.stageRoot.spatial().scale *= Vector3f(1f, 1f, 2f)

        val hullbox = Box(Vector3f(20.0f, 20.0f, 20.0f), insideNormals = true)
        hullbox.name = "hullbox"
        hullbox.material {
            ambient = Vector3f(0.6f, 0.6f, 0.6f)
            diffuse = Vector3f(0.4f, 0.4f, 0.4f)
            specular = Vector3f(0.0f, 0.0f, 0.0f)
            cullingMode = Material.CullingMode.Front
        }
        scene.addChild(hullbox)

        thread {
            //Thread.sleep(5000)
            val db = DemoBehavior(
                hw.side.toFloat(),
                stageSpaceManager
            )
            db.fixedStack()
            Thread.sleep(2500)
            db.fixed()
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

