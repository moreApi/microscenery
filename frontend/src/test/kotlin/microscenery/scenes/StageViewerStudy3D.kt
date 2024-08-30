package microscenery.scenes

import fromScenery.utils.extensions.times
import graphics.scenery.Box
import graphics.scenery.attribute.material.Material
import graphics.scenery.volumes.TransferFunction
import microscenery.DefaultScene
import microscenery.DemoMicroscopeHardware
import microscenery.MicrosceneryHub
import microscenery.MicroscenerySettings
import microscenery.UI.StageSpaceUI
import microscenery.scenes.microscope.DemoBehavior
import microscenery.simulation.Simulatable
import microscenery.simulation.SimulationMicroscopeHardware
import microscenery.stageSpace.MicroscopeLayout
import microscenery.stageSpace.StageSpaceManager
import org.joml.Matrix4f
import org.joml.Vector2i
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.sqrt


class StageViewerStudy3D : DefaultScene(withSwingUI = true, width = 500, height = 500) {
    lateinit var stageSpaceManager: StageSpaceManager
    val msHub = MicrosceneryHub(hub)

    override fun init() {
        super.init()
        logger.info("Starting demo hw scene")
        cam.spatial().position = Vector3f(0f, 0f, 2f)


        MicroscenerySettings.set("Stage.precisionXY", 1f)
        MicroscenerySettings.set("Stage.precisionZ", 1f)

        val box = Box(Vector3f(10f))
        box.material().cullingMode = Material.CullingMode.FrontAndBack
        box.addAttribute(Simulatable::class.java, object : Simulatable {
            val maxIntensity = 4000.toShort()
            val parent = box
            override fun intensity(pos: Vector3f): Short {
                val modelPos = Vector4f(pos,1f).mul(Matrix4f(parent.spatial().model).invertAffine())
                val boxMin = Vector3f(-0.5f) * box.sizes
                val boxMax = Vector3f(0.5f) * box.sizes



                val dx = listOf(boxMin.x - modelPos.x, 0f, modelPos.x - boxMax.x).max()
                val dy = listOf(boxMin.y - modelPos.y, 0f, modelPos.y - boxMax.y).max();
                val dz = listOf(boxMin.z - modelPos.z, 0f, modelPos.z - boxMax.z).max();
                val dist = sqrt(dx*dx + dy*dy + dz * dz)


                return when{
                    dist <= 0f -> maxIntensity
                    dist > 15f -> 0
                    else -> ((1f-(dist/15f)) * maxIntensity).toInt().toShort()
                }
            }
        })


        val hw = SimulationMicroscopeHardware(msHub, imageSize = Vector2i(50))
        stageSpaceManager = StageSpaceManager(
            hw,
            scene,
            msHub,
            layout = MicroscopeLayout.Default(MicroscopeLayout.Axis.Z)
        )
        stageSpaceManager.stageRoot.addChild(box)

        val tfManager = stageSpaceManager.sliceManager.transferFunctionManager

        tfManager.transferFunction = TransferFunction.ramp(distance = 1f)
        tfManager.minDisplayRange = 0f
        tfManager.maxDisplayRange = 5000f

        thread {
            @Suppress("UNUSED_VARIABLE") val db = DemoBehavior(
                hw.hardwareDimensions().stageMax,
                stageSpaceManager
            )
            db.randomStatic(10)
        }
        thread {
            while (true) {
                Thread.sleep(200)
                scene to stageSpaceManager
            }
        }
    }

    override fun inputSetup() {
        super.inputSetup()
        StageSpaceUI(stageSpaceManager).stageUI(this, inputHandler, msHub)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            StageViewerStudy3D().main()
        }
    }
}


