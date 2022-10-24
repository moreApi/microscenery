package microscenery.example

import graphics.scenery.Box
import graphics.scenery.attribute.material.Material
import graphics.scenery.utils.extensions.times
import graphics.scenery.volumes.TransferFunction
import microscenery.*
import microscenery.hardware.MicroscopeHardware
import microscenery.hardware.micromanagerConnection.MMConnection
import microscenery.hardware.micromanagerConnection.MicromanagerWrapper
import microscenery.signals.ServerState
import org.joml.Vector3f
import kotlin.concurrent.thread

class LocalMMScene: DefaultScene() {

    init {

        val hardware: MicroscopeHardware = MicromanagerWrapper(MMConnection())
        val stageSpaceManager = StageSpaceManager(hardware, scene)

        stageSpaceManager.stageRoot.spatial().scale *= Vector3f(1f,1f,2f)


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

        stageSpaceManager.snapSlice(Vector3f(0f,0f,0f))
        stageSpaceManager.snapSlice(Vector3f(0f,0f,10f))
        stageSpaceManager.snapSlice(Vector3f(0f,0f,20f))
        stageSpaceManager.snapSlice(Vector3f(0f,0f,30f))

        thread {
            while (true){
                Thread.sleep(200)
                scene
            }
        }
    }

    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            LocalMMScene().main()
        }
    }

}