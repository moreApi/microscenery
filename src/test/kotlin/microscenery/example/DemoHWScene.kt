package microscenery.example

import graphics.scenery.Box
import graphics.scenery.attribute.material.Material
import graphics.scenery.utils.extensions.times
import microscenery.*
import org.joml.Vector3f
import kotlin.concurrent.thread

class DemoHWScene: DefaultScene() {

    init {

        val hw = DemoMicroscopeHardware()
        val stageSpaceManager = StageSpaceManager(hw,scene)

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


        for (z in listOf(0,50,100,150,199))
            for (y in listOf(0,50,100,150))
                for (x in listOf(0,50,100,150)){
                    val target = Vector3f(x.toFloat(),y.toFloat(),z.toFloat())
                    stageSpaceManager.snapSlice(target)
                }

        /*
        for(x in 0..3) {
            for(y in 0..3) {
                for (z in 0..3) {
                    //val target = Random.random3DVectorFromRange(0f, hw.side.toFloat())
                    //target.z = z * 20f
                    val target = Vector3f(x*50f,y*50f,z*50f)
                    stageSpaceManager.snapSlice(target)
                    val box = Box()
                    box.position = target
                    stageSpaceManager.stageRoot.addChild(box)
                }
            }
        }

         */



/*
        var target = Vector3f(0f,0f,50f)
        stageSpaceManager.snapSlice(target)
        target = Vector3f(0f,0f,51f)
        stageSpaceManager.snapSlice(target)
        stageSpaceManager.stageRoot.spatial().scale *= Vector3f(1f,1f,4f)
 */

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