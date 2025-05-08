package microscenery.scenes.microscope

import graphics.scenery.Node
import graphics.scenery.utils.extensions.times
import microscenery.*
import microscenery.UI.StageSpaceUI
import microscenery.UI.UIModel
import microscenery.signals.MicroscopeControlSignal
import microscenery.simulation.AblationSimulationMicroscope
import microscenery.stageSpace.MicroscopeLayout
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector3f
import kotlin.concurrent.thread


class DemoHWScene : DefaultScene(withSwingUI = true) {
    lateinit var stageSpaceManager: StageSpaceManager
    val msHub = MicrosceneryHub(hub)

    override fun init() {
        super.init()
        logger.info("Starting demo hw scene")
        cam.spatial().position = Vector3f(0f, 0f, 2f)


        MicroscenerySettings.set("Stage.precisionXY", 1f)
        MicroscenerySettings.set("Stage.precisionZ", 1f)
        MicroscenerySettings.get(Settings.Ablation.SizeUM,30f)


        val hw = AblationSimulationMicroscope(DemoMicroscopeHardware(binning = 1, size = Vector3f(100f),seed = 1337))
        stageSpaceManager = StageSpaceManager(
            hw,
            scene,
            msHub,
            layout = MicroscopeLayout.Default(MicroscopeLayout.Axis.Z)
//            layout = MicroscopeLayout.Scape(MicroscopeLayout.Axis.Y, 33f)
        )

        stageSpaceManager.stageRoot.spatial().scale *= Vector3f(1f, 1f, 1f)

        //tfUI.name = "Slices"

        thread {
            //Thread.sleep(5000)
            @Suppress("UNUSED_VARIABLE") val db = DemoBehavior(
                hw.hardwareDimensions().stageMax,
                stageSpaceManager
            )

            val points = (0 until 5).flatMap { y ->
                (0 until 5).map { x ->
                        Vector3f(x*20f, y*20f,0f)
                }
            }
            val ablPoints = points.map {
                MicroscopeControlSignal.AblationPoint(position = it)
            }

            hw.ablatePoints(MicroscopeControlSignal.AblationPoints(ablPoints))

            db.positions(*points.toTypedArray())




            //db.fixedStack(Vector3f(100f,100f,000f), Vector3f(100f, 100f,200f))
            //Thread.sleep(2500)
            //db.randomStatic(10)
            //db.fixed()
            //db.fixedStack(Vector3f(150f,150f,100f),Vector3f(150f,150f,150f))
            stageSpaceManager.sliceManager.transferFunctionManager.maxDisplayRange = 1000f


            //stageSpaceManager.sampleStageSpace(Vector3f(25f), Vector3f(175f), Vector3f(30f, 30f, 50f))
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

        StageSpaceUI(stageSpaceManager).stageUI(this, inputHandler,msHub)

        msHub.getAttribute(UIModel::class.java).changeEvents += {
            when(it.kProperty){
                UIModel::selected -> println("${(it.new as? Node)?.name ?: "none"} selected")
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            DemoHWScene().main()
        }
    }
}


