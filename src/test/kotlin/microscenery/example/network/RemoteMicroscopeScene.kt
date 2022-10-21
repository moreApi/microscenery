package microscenery.example.network

import graphics.scenery.Box
import graphics.scenery.attribute.material.Material
import graphics.scenery.numerics.Random
import graphics.scenery.utils.extensions.times
import graphics.scenery.volumes.TransferFunction
import microscenery.DefaultScene
import microscenery.StageSpaceManager
import microscenery.lightSleepOnCondition
import microscenery.network.RemoteMicroscopeClient
import microscenery.signals.ServerState
import org.joml.Vector3f
import org.zeromq.ZContext
import kotlin.concurrent.thread


class RemoteMicroscopeScene : DefaultScene() {
    init {
        val zContext = ZContext()
        val client = RemoteMicroscopeClient(zContext = zContext)
        val stageSpaceManager = StageSpaceManager(client, scene)

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

        lightSleepOnCondition { stageSpaceManager.hardware.status().state == ServerState.MANUAL }
        lightSleepOnCondition { stageSpaceManager.hardware.hardwareDimensions().imageSize.x != 0 }
/*
        while (stageSpaceManager.hardware.status().state != ServerState.MANUAL){
            Thread.sleep(500)
            println(stageSpaceManager.hardware.status().state)
        }
        while (stageSpaceManager.hardware.hardwareDimensions().imageSize.x == 0){
            Thread.sleep(500)
            println(stageSpaceManager.hardware.hardwareDimensions().stageMin)
        }
 */
        var shouldBeHere = 0
        val randomSlices = true
        if (randomSlices) {
            for (i in 0..200) {
                //println("Requesting slice #$i")
                val target = Random.random3DVectorFromRange(0f, client.hardwareDimensions().stageMax.x)
                //Thread.sleep(50)
                stageSpaceManager.snapSlice(target)
                shouldBeHere++

            }
        } else {
            for (z in listOf(0, 50, 100, 150, 199)) for (y in listOf(0, 50, 100, 150)) for (x in listOf(
                0,
                50,
                100,
                150
            )) {
                val target = Vector3f(x.toFloat(), y.toFloat(), z.toFloat())
                stageSpaceManager.snapSlice(target)
                shouldBeHere++
                //Thread.sleep(10)
            }
        }


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
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            RemoteMicroscopeScene().main()
        }
    }

}
