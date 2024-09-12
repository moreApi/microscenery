package microscenery.scenes.stageStudy

import graphics.scenery.Box
import graphics.scenery.Node
import graphics.scenery.RichNode
import graphics.scenery.Sphere
import graphics.scenery.attribute.material.Material
import microscenery.detach
import microscenery.simulation.BoxSimulatable
import microscenery.simulation.SphereSimulatable
import org.joml.Vector3f

object StageSimulation {

    fun scaffold(stageRoot: Node){
        val boxes = listOf(
            Vector3f(10f,500f,10f) to Vector3f(0f,0f,0f),
            Vector3f(500f,30f,10f) to Vector3f(0f,0f,0f),
            Vector3f(30f,30f,300f) to Vector3f(0f,0f,0f),
            Vector3f(30f,20f,200f) to Vector3f(250f,0f,0f),
            Vector3f(30f,20f,200f) to Vector3f(-250f,0f,0f),
            Vector3f(300f,20f,20f) to Vector3f(0f,250f,0f),
            Vector3f(30f,20f,200f) to Vector3f(0f,-250f,0f),
        )
        val droot = RichNode("droot").also { stageRoot.addChild(it) }
        //        val droot = scene.find("droot")
        droot.children.forEach { it.detach() }

        boxes.forEach {
            val box = Box(it.first)
            box.spatial().position = it.second

            box.material().cullingMode = Material.CullingMode.FrontAndBack
            BoxSimulatable.addTo(box).also {
                it.range = 50f
                it.maxIntensity = 4000
            }
            droot.addChild(box)
        }
    }

    fun lightBulb(stageRoot: Node){

        val box = Box(Vector3f(5f,10f,5f))
        box.material().cullingMode = Material.CullingMode.FrontAndBack
        BoxSimulatable.addTo(box).also {
            it.range = 5f
            it.maxIntensity = 4000
        }
        stageRoot.addChild(box)

        val sphere = Sphere(10f)
        sphere.material().cullingMode = Material.CullingMode.FrontAndBack
        sphere.spatial().position.y = 15f
        SphereSimulatable.addTo(sphere)
        stageRoot.addChild(sphere)
    }

}