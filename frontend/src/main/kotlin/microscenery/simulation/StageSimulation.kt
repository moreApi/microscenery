package microscenery.simulation

import fromScenery.utils.extensions.plus
import fromScenery.utils.extensions.times
import graphics.scenery.Box
import graphics.scenery.Node
import graphics.scenery.Scene
import graphics.scenery.Sphere
import graphics.scenery.attribute.material.Material
import microscenery.MicrosceneryHub
import microscenery.MicroscenerySettings
import microscenery.Settings.StageSpace.HideStageSpaceLabel
import microscenery.discover
import microscenery.stageSpace.FocusManager
import microscenery.stageSpace.MicroscopeLayout
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector2i
import org.joml.Vector3f

class StageSimulation(val stageSpaceSize: Float = 1000f, val imageSize: Int = 150) {
    lateinit var stageSpaceManager: StageSpaceManager

    fun setupStage(msHub: MicrosceneryHub, scene: Scene): StageSpaceManager {
        MicroscenerySettings.set(HideStageSpaceLabel, true)

        val hw = SimulationMicroscopeHardware(
            msHub,
            stageSize = Vector3f(stageSpaceSize),
            imageSize = Vector2i(imageSize),
            maxIntensity = Short.MAX_VALUE
        )
        stageSpaceManager = StageSpaceManager(
            hw,
            scene,
            msHub,
            layout = MicroscopeLayout.Default(MicroscopeLayout.Axis.Z)
        )

        stageSpaceManager.focusManager.mode = FocusManager.Mode.STEERING
        stageSpaceManager.goLive()

        return stageSpaceManager
    }

    interface Scenario {
        fun generate(stageSpaceManager: StageSpaceManager, stageSpaceSize: Float): List<Vector3f>
    }


    fun scaffold(stageRoot: Node): List<Vector3f> {
        // size to pos
        val boxes = listOf(
            Vector3f(10f, 500f, 10f) to Vector3f(0f, 0f, 0f),
            Vector3f(500f, 30f, 10f) to Vector3f(0f, 0f, 0f),
            Vector3f(30f, 30f, 300f) to Vector3f(0f, 0f, 0f),
            Vector3f(30f, 20f, 200f) to Vector3f(250f, 0f, 0f),
            Vector3f(30f, 20f, 200f) to Vector3f(-250f, 0f, 0f),
            Vector3f(300f, 20f, 20f) to Vector3f(0f, 250f, 0f),
            Vector3f(30f, 20f, 200f) to Vector3f(0f, -250f, 0f),
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

    fun lightBulb(stageRoot: Node) {

        val box = Box(Vector3f(5f, 10f, 5f))
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

    companion object {

        fun Scene.toggleMaterialRendering() {
            this.discover { node ->
                node.ifHasAttribute(Simulatable::class.java) {
                    node.ifMaterial {
                        if (this.cullingMode == Material.CullingMode.FrontAndBack)
                            node.showMaterial()
                        else
                            node.hideMaterial()
                    }
                }
                false
            }
        }

        fun Node.showMaterial() {
            ifMaterial { cullingMode = Material.CullingMode.None }
        }

        fun Node.hideMaterial() {
            ifMaterial { cullingMode = Material.CullingMode.FrontAndBack }
        }
    }
}


