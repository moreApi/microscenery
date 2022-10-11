package microscenery.example.network

import graphics.scenery.Box
import graphics.scenery.attribute.material.Material
import graphics.scenery.numerics.Random
import graphics.scenery.utils.extensions.times
import microscenery.*
import microscenery.hardware.DemoMicroscopeHardware
import microscenery.network.RemoteMicroscopeClient
import microscenery.network.RemoteMicroscopeServer
import microscenery.network.SliceStorage
import org.joml.Vector3f
import org.zeromq.ZContext
import kotlin.concurrent.thread

class RemoteMicroscopeLocalhostScene: DefaultScene() {

    init {
        val zContext = ZContext()

        val microscope = DemoMicroscopeHardware()
        @Suppress("UNUSED_VARIABLE")
        val server = RemoteMicroscopeServer(microscope, storage = SliceStorage(500*1024*1024), zContext = zContext)

        val client = RemoteMicroscopeClient(storage = SliceStorage(500*1024*1024),zContext = zContext)
        val stageSpaceManager = StageSpaceManager(client,scene)

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


        var shouldBeHere = 0
        val randomSlices = true
        if (randomSlices){
            for (i in 0 .. 200) {
                //println("Requesting slice #$i")
                val target = Random.random3DVectorFromRange(0f, microscope.side.toFloat())
                //Thread.sleep(50)
                stageSpaceManager.snapSlice(target)
                shouldBeHere++

            }
        } else {
            for (z in listOf(0, 50, 100, 150, 199))
                for (y in listOf(0, 50, 100, 150))
                    for (x in listOf(0, 50, 100, 150)) {
                        val target = Vector3f(x.toFloat(), y.toFloat(), z.toFloat())
                        stageSpaceManager.snapSlice(target)
                        shouldBeHere++
                        //Thread.sleep(10)
                    }
        }


        thread {
            while (true){
                Thread.sleep(200)
                scene
                println("$shouldBeHere, ${stageSpaceManager.stageRoot.children.size}")
            }
        }
    }

    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            RemoteMicroscopeLocalhostScene().main()
        }
    }

}