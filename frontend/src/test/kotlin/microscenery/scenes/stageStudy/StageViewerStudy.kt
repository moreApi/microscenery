package microscenery.scenes.stageStudy

import graphics.scenery.volumes.TransferFunction
import microscenery.*
import microscenery.UI.StageSpaceUI
import microscenery.scenes.microscope.DemoBehavior
import microscenery.simulation.SimulationMicroscopeHardware
import microscenery.stageSpace.MicroscopeLayout
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector2i
import org.joml.Vector3f
import kotlin.concurrent.thread


class StageViewerStudy : DefaultScene(withSwingUI = true) {
    lateinit var stageSpaceManager: StageSpaceManager
    val msHub = MicrosceneryHub(hub)

    override fun init() {
        super.init()
        logger.info("Starting demo hw scene")
        cam.spatial().position = Vector3f(0f, 0f, 2f)


        MicroscenerySettings.set("Stage.precisionXY", 1f)
        MicroscenerySettings.set("Stage.precisionZ", 1f)
        MicroscenerySettings.set(Settings.UI.ShowSelectionIndicator, false)


        val hw = SimulationMicroscopeHardware(msHub, stageSize = Vector3f(500f), imageSize = Vector2i(50), maxIntensity = 4000)
        stageSpaceManager = StageSpaceManager(
            hw,
            scene,
            msHub,
            layout = MicroscopeLayout.Default(MicroscopeLayout.Axis.Z)
        )
        StageSimulation.scaffold(stageSpaceManager.stageRoot)

        stageSpaceManager.sliceManager.transferFunctionManager.apply {
            this.transferFunction = TransferFunction.flat(1f)
            maxDisplayRange = 4100f
            minDisplayRange = 0f
        }

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
            StageViewerStudy().main()
        }
    }
}


