package microscenery.scenes.microscope

import microscenery.*
import microscenery.UI.StageSpaceUI
import microscenery.hardware.MicroscopeHardware
import microscenery.hardware.micromanagerConnection.MMCoreConnector
import microscenery.hardware.micromanagerConnection.MicromanagerWrapper
import microscenery.signals.ServerState
import microscenery.stageSpace.MicroscopeLayout
import microscenery.stageSpace.StageSpaceManager
import mmcorej.CMMCore
import org.joml.Vector3f
import kotlin.concurrent.thread

/**
 * Working directory needs to be the MM dir.
 */
class LocalMMScene : DefaultScene(withSwingUI = true) {
    lateinit var stageSpaceManager: StageSpaceManager
    val msHub = MicrosceneryHub(hub)

    override fun init() {
        super.init()
        cam.spatial().position = Vector3f(0f, 0f, 5f)

//        MicroscenerySettings.set("Stage.minX", 40000f)
//        MicroscenerySettings.set("Stage.minY", 22000f)
//        MicroscenerySettings.set("Stage.minZ", -50f)
//        MicroscenerySettings.set("Stage.maxX", 44000f)
//        MicroscenerySettings.set("Stage.maxY", 26000f)
//        MicroscenerySettings.set("Stage.maxZ", 100f)
//        val stageStart =Vector3f(41000f, 23000f, 0f)

        MicroscenerySettings.set("Stage.minX", -300f)
        MicroscenerySettings.set("Stage.minY", -300f)
        MicroscenerySettings.set("Stage.minZ", -250f)
        MicroscenerySettings.set("Stage.maxX", 300f)
        MicroscenerySettings.set("Stage.maxY", 300f)
        MicroscenerySettings.set("Stage.maxZ", 250f)

        val stageStart = Vector3f()

        val hardware: MicroscopeHardware =
            MicromanagerWrapper(MMCoreConnector(initLocalMMCoreDemo()))//initLocalMMCoreFake(SkewedC1HPO)).apply { moveStage(stageStart, false) })
        stageSpaceManager = StageSpaceManager(
            hardware, scene, msHub, layout = MicroscopeLayout.Scape(MicroscopeLayout.Axis.Z, -0.5)
        )

        lightSleepOnCondition { hardware.status().state == ServerState.MANUAL }

//        stageSpaceManager.stageRoot.spatial().position = stageSpaceManager.stageAreaCenter

        //stageSpaceManager.focusTarget?.mode = FocusFrame.Mode.STEERING
        @Suppress("UNUSED_VARIABLE")
        val db = DemoBehavior(hardware.hardwareDimensions().stageMax, stageSpaceManager)
        //Thread.sleep(2000)
        //db.randomLive()
        //db.fixedStack(Vector3f(0f, 0f, 0f), Vector3f(0f, 0f, 120f))
//        db.positions(
//            Vector3f(0f,0f,20f),
//            Vector3f(0f,0f,30f),
//            Vector3f(0f,0f,40f),
//            Vector3f(0f,0f,50f),
//            Vector3f(0f,0f,60f),
//            Vector3f(0f,0f,70f),
//            Vector3f(0f,0f,80f),
//        )


        thread {
            while (true) {
                Thread.sleep(200)
                scene
                stageSpaceManager
            }
        }
    }

    override fun inputSetup() {
        super.inputSetup()
        StageSpaceUI(stageSpaceManager).stageUI(this,inputHandler, msHub)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            LocalMMScene().main()
        }

        val CHERRY = "TiffStack_16_Cherry_time"
        val SkewedC1HPO = "Skewed-C1-HPO"

        fun initLocalMMCoreFake(mmPresetName: String = CHERRY): CMMCore {
            val core = CMMCore()
            val mmConfiguration = "C:/Program Files/Micro-Manager-2.0gamma/MMConfig_fake.cfg"
            core.loadSystemConfiguration(mmConfiguration)

            val mmSettingsGroupName = "FakeCam"
            println("Setting $mmSettingsGroupName to $mmPresetName")
            core.setConfig(mmSettingsGroupName, mmPresetName)
            return core
        }
        fun initLocalMMCoreDemo(): CMMCore {
            val core = CMMCore()
            val mmConfiguration = "C:/Program Files/Micro-Manager-2.0/MMConfig_demo.cfg"
            core.loadSystemConfiguration(mmConfiguration)

            return core
        }
    }

}

