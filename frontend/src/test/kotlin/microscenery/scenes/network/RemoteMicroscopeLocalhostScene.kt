package microscenery.scenes.network

import graphics.scenery.Box
import graphics.scenery.attribute.material.Material
import graphics.scenery.utils.extensions.times
import microscenery.*
import microscenery.scenes.microscope.DemoBehavior
import microscenery.network.RemoteMicroscopeClient
import microscenery.network.RemoteMicroscopeServer
import microscenery.network.SliceStorage
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector3f
import org.zeromq.ZContext
import kotlin.concurrent.thread

class RemoteMicroscopeLocalhostScene : DefaultScene() {

    init {
        val zContext = ZContext()

        val microscope = DemoMicroscopeHardware()

        @Suppress("UNUSED_VARIABLE")
        val server = RemoteMicroscopeServer(microscope, storage = SliceStorage(500 * 1024 * 1024), zContext = zContext)

        val client = RemoteMicroscopeClient(zContext = zContext)
        val msHub = MicrosceneryHub(hub)
        val stageSpaceManager = StageSpaceManager(client, scene, msHub)

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


        DemoBehavior(microscope.dataSide.toFloat(), stageSpaceManager).randomLive()

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
            RemoteMicroscopeLocalhostScene().main()
        }
    }

}