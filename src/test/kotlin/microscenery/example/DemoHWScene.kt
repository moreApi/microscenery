package microscenery.example

import graphics.scenery.Box
import graphics.scenery.attribute.material.Material
import graphics.scenery.numerics.Random
import graphics.scenery.utils.extensions.times
import graphics.scenery.volumes.TransferFunction
import microscenery.*
import microscenery.hardware.DemoMicroscopeHardware
import org.joml.Vector3f
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

class DemoHWScene: DefaultScene() {

    init {

        val hw = DemoMicroscopeHardware()
        val stageSpaceManager = StageSpaceManager(hw, scene)

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


        val sortedSlices = ArrayList<Vector3f>()
        val randomSlices = true
        if (randomSlices){
            for (i in 0 .. 200) {
                val target = Random.random3DVectorFromRange(0f, hw.side.toFloat())
                sortedSlices.add(target)
            }
        } else {
            sortedSlices.ensureCapacity(13)
            for (z in listOf(0, 50, 100, 150, 199))
                for (y in listOf(0, 50, 100, 150))
                    for (x in listOf(0, 50, 100, 150)) {
                        val target = Vector3f(x.toFloat(), y.toFloat(), z.toFloat())
                        sortedSlices.add(target)
                    }
        }
        sortedSlices.sortBy { it.z() }
        for(target in sortedSlices)
        {
            stageSpaceManager.snapSlice(target)
        }

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
            DemoHWScene().main()
        }
    }

}