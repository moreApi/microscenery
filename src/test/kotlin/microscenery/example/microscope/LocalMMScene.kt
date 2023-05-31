package microscenery.example.microscope

import microscenery.*
import microscenery.UI.StageSpaceUI
import microscenery.hardware.MicroscopeHardware
import microscenery.hardware.micromanagerConnection.MMConnection
import microscenery.hardware.micromanagerConnection.MicromanagerWrapper
import microscenery.signals.ServerState
import microscenery.stageSpace.MicroscopeLayout
import microscenery.stageSpace.StageSpaceManager
import mmcorej.CMMCore
import org.joml.Vector3f
import kotlin.concurrent.thread

class LocalMMScene : DefaultScene(withSwingUI = true) {
    val stageSpaceManager: StageSpaceManager

    init {
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
            MicromanagerWrapper(MMConnection(initLocalMMCoreFake()).apply { moveStage(stageStart, false) })
        stageSpaceManager = StageSpaceManager(
            hardware, scene, hub, addFocusFrame = true, layout = MicroscopeLayout.Default(MicroscopeLayout.Axis.Z)
        )

        lightSleepOnCondition { hardware.status().state == ServerState.MANUAL }

//        stageSpaceManager.stageRoot.spatial().position = stageSpaceManager.stageAreaCenter

        //stageSpaceManager.focusTarget?.mode = FocusFrame.Mode.STEERING
        @Suppress("UNUSED_VARIABLE")
        val db = DemoBehavior(hardware.hardwareDimensions().stageMax.z, stageSpaceManager)
        //Thread.sleep(2000)
        //db.randomLive()
        //db.fixedStack(Vector3f(0f, 0f, 0f), Vector3f(0f, 0f, 120f))
        //db.fixed()

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
        StageSpaceUI(stageSpaceManager).stageUI(this,inputHandler)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            LocalMMScene().main()
        }

        fun initLocalMMCoreFake(): CMMCore {
            val core = CMMCore()
            val mmConfiguration = "C:/Program Files/Micro-Manager-2.0gamma/MMConfig_fake.cfg"
            core.loadSystemConfiguration(mmConfiguration)

            val mmSettingsGroupName = "FakeCam"
            val mmPresetName = "TiffStack_16_Cherry_time"
            println("Setting $mmSettingsGroupName to $mmPresetName")
            core.setConfig(mmSettingsGroupName, mmPresetName)
            return core
        }
        fun initLocalMMCoreDemo(): CMMCore {
            val core = CMMCore()
            val mmConfiguration = "C:/Program Files/Micro-Manager-2.0gamma/MMConfig_demo.cfg"
            core.loadSystemConfiguration(mmConfiguration)

            return core
        }
    }

}

