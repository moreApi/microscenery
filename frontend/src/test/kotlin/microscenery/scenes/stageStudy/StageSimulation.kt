package microscenery.scenes.stageStudy

import fromScenery.utils.extensions.plus
import fromScenery.utils.extensions.times
import graphics.scenery.*
import graphics.scenery.attribute.material.Material
import graphics.scenery.primitives.Cylinder
import graphics.scenery.volumes.TransferFunction
import microscenery.MicrosceneryHub
import microscenery.detach
import microscenery.simulation.BoxSimulatable
import microscenery.simulation.CylinderSimulatable
import microscenery.simulation.Simulatable.Companion.hideMaterial
import microscenery.simulation.SimulationMicroscopeHardware
import microscenery.simulation.SphereSimulatable
import microscenery.stageSpace.FocusManager
import microscenery.stageSpace.MicroscopeLayout
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector2i
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object StageSimulation {

    fun setupStage(msHub: MicrosceneryHub, scene: Scene):StageSpaceManager{
        val hw = SimulationMicroscopeHardware(msHub, stageSize = Vector3f(600f), imageSize = Vector2i(150), maxIntensity = 4000)
        val stageSpaceManager = StageSpaceManager(
            hw,
            scene,
            msHub,
            layout = MicroscopeLayout.Default(MicroscopeLayout.Axis.Z)
        )

        stageSpaceManager.sliceManager.transferFunctionManager.apply {
            maxDisplayRange = 4100f
            minDisplayRange = 0f
        }

        stageSpaceManager.focusManager.mode = FocusManager.Mode.STEERING
        stageSpaceManager.goLive()

        return stageSpaceManager
    }

    fun tube(stageRoot: Node): List<Vector3f>{
        val radius = 200f
        val height = 400f
        Cylinder(radius * 0.95f,height, 16).let { cy ->
            CylinderSimulatable.addTo(cy).maxIntensity = 3000
            cy.hideMaterial()
            cy.spatial().position = Vector3f(0f,-height*0.5f,0f)
            stageRoot.addChild(cy)
        }

        Cylinder(radius,height, 16).let { cy ->
            CylinderSimulatable.addTo(cy).maxIntensity = 3000
            cy.hideMaterial()
            cy.spatial().position = Vector3f(0f,-height*0.5f,0f)
            stageRoot.addChild(cy)
        }

        return (0..10).map {
            val a = Random.nextFloat()
            Vector3f(
                radius*0.8f * cos(a*2f*Math.PI.toFloat()),
                (Random.nextFloat()-0.5f)*height,
                radius*0.8f * sin(a*2f*Math.PI.toFloat())
            )
        }
    }

    fun scaffold(stageRoot: Node): List<Vector3f>{
        // size to pos
        val boxes = listOf(
            Vector3f(10f,500f,10f) to Vector3f(0f,0f,0f),
            Vector3f(500f,30f,10f) to Vector3f(0f,0f,0f),
            Vector3f(30f,30f,300f) to Vector3f(0f,0f,0f),
            Vector3f(30f,20f,200f) to Vector3f(250f,0f,0f),
            Vector3f(30f,20f,200f) to Vector3f(-250f,0f,0f),
            Vector3f(300f,20f,20f) to Vector3f(0f,250f,0f),
            Vector3f(30f,20f,200f) to Vector3f(0f,-250f,0f),
        )

        boxes.forEach {
            val box = Box(it.first)
            box.spatial().position = it.second

            box.material().cullingMode = Material.CullingMode.FrontAndBack
            BoxSimulatable.addTo(box).also {
                it.range = 50f
                it.maxIntensity = 4000
            }
            stageRoot.addChild(box)
        }

        return boxes.drop(3).flatMap {
            listOf(it.first * 0.5f + it.second, it.first * -0.5f + it.second)
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