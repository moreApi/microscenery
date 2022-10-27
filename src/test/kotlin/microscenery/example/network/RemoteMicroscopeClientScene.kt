package microscenery.example.network

import graphics.scenery.Box
import graphics.scenery.attribute.material.Material
import microscenery.DefaultScene
import microscenery.StageSpaceManager
import microscenery.example.DemoBehaviorMode
import microscenery.example.DemoHWScene
import microscenery.lightSleepOnCondition
import microscenery.network.RemoteMicroscopeClient
import microscenery.signals.ServerState
import org.joml.Vector3f
import org.zeromq.ZContext


class RemoteMicroscopeClientScene : DefaultScene() {
    init {
        val zContext = ZContext()
        val client = RemoteMicroscopeClient(zContext = zContext)
        val stageSpaceManager = StageSpaceManager(client, scene, addFocusFrame = true)

        //stageSpaceManager.stageRoot.spatial().scale *= Vector3f(1f, 1f, 2f)


        val hullbox = Box(Vector3f(20.0f, 20.0f, 20.0f), insideNormals = true)
        hullbox.name = "hullbox"
        hullbox.material {
            ambient = Vector3f(0.6f, 0.6f, 0.6f)
            diffuse = Vector3f(0.4f, 0.4f, 0.4f)
            specular = Vector3f(0.0f, 0.0f, 0.0f)
            cullingMode = Material.CullingMode.Front
        }
        scene.addChild(hullbox)

        lightSleepOnCondition { stageSpaceManager.hardware.status().state == ServerState.MANUAL }
        lightSleepOnCondition { stageSpaceManager.hardware.hardwareDimensions().imageSize.x != 0 }

        DemoHWScene.demoBehavior(DemoBehaviorMode.RandomLive,50f,stageSpaceManager)


        /*
        thread {
            while (true) {
                Thread.sleep(200)
                scene
                @Suppress("UNUSED_EXPRESSION")
                client
                if (shouldBeHere != stageSpaceManager.stageRoot.children.size)
                    println("$shouldBeHere, ${stageSpaceManager.stageRoot.children.size}")
            }
        }
           */

    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            RemoteMicroscopeClientScene().main()
        }
    }

}
