package microscenery.example.microscope

import graphics.scenery.Box
import graphics.scenery.attribute.material.Material
import microscenery.DefaultScene
import microscenery.StageSpaceManager
import microscenery.hardware.MicroscopeHardware
import microscenery.hardware.micromanagerConnection.MMConnection
import microscenery.hardware.micromanagerConnection.MicromanagerWrapper
import microscenery.lightSleepOnCondition
import microscenery.signals.ServerState
import org.joml.Vector3f
import kotlin.concurrent.thread

class LocalMMScene : DefaultScene() {

    init {

        val hardware: MicroscopeHardware = MicromanagerWrapper(MMConnection())
        val stageSpaceManager = StageSpaceManager(hardware, scene, addFocusFrame = true)

        val hullbox = Box(Vector3f(20.0f, 20.0f, 20.0f), insideNormals = true)
        hullbox.name = "hullbox"
        hullbox.material {
            ambient = Vector3f(0.6f, 0.6f, 0.6f)
            diffuse = Vector3f(0.4f, 0.4f, 0.4f)
            specular = Vector3f(0.0f, 0.0f, 0.0f)
            cullingMode = Material.CullingMode.Front
        }
        scene.addChild(hullbox)


        lightSleepOnCondition { hardware.status().state == ServerState.MANUAL }

        //stageSpaceManager.snapSlice(Vector3f(0f,0f,0f))
        //stageSpaceManager.snapSlice(Vector3f(10f))
        //stageSpaceManager.snapSlice(Vector3f(20f))
        //stageSpaceManager.stagePosition = Vector3f(50f)
        //stageSpaceManager.snapSlice()
        //stageSpaceManager.snapSlice(Vector3f(0f,0f,30f))

        DemoBehavior(hardware.hardwareDimensions().stageMax.length(), stageSpaceManager).randomLive()

        thread {
            while (true) {
                Thread.sleep(200)
                scene
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            LocalMMScene().main()
        }
    }

}