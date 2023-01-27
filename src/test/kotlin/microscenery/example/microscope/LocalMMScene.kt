package microscenery.example.microscope

import graphics.scenery.Box
import graphics.scenery.attribute.material.Material
import graphics.scenery.volumes.TransferFunctionEditor
import microscenery.*
import microscenery.UI.StageSpaceUI.Companion.stageUserInteraction
import microscenery.hardware.MicroscopeHardware
import microscenery.hardware.micromanagerConnection.MMConnection
import microscenery.hardware.micromanagerConnection.MicromanagerWrapper
import microscenery.signals.ServerState
import microscenery.stageSpace.MicroscopeLayout
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector3f
import kotlin.concurrent.thread

class LocalMMScene : DefaultScene() {
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

        MicroscenerySettings.set("Stage.minX", 0f)
        MicroscenerySettings.set("Stage.minY", 0f)
        MicroscenerySettings.set("Stage.minZ", 0f)
        MicroscenerySettings.set("Stage.maxX", 0f)
        MicroscenerySettings.set("Stage.maxY", 0f)
        MicroscenerySettings.set("Stage.maxZ", 120f)

        MicroscenerySettings.set("MMConnection.core.configuration","C:/Program Files/Micro-Manager-2.0gamma/MMConfig_fake.cfg")

        val stageStart = Vector3f()

        val hardware: MicroscopeHardware =
            MicromanagerWrapper(MMConnection().apply { moveStage(stageStart, false) })
        stageSpaceManager = StageSpaceManager(
            hardware, scene, hub, addFocusFrame = true, scaleDownFactor = 50f,
            layout = MicroscopeLayout.Default(MicroscopeLayout.Axis.Z)
        )

        val hullbox = Box(Vector3f(20.0f, 20.0f, 20.0f), insideNormals = true)
        hullbox.name = "hullbox"
        hullbox.material {
            ambient = Vector3f(0.6f, 0.6f, 0.6f)
            diffuse = Vector3f(0.4f, 0.4f, 0.4f)
            specular = Vector3f(0.0f, 0.0f, 0.0f)
            cullingMode = Material.CullingMode.Front
        }
        scene.addChild(hullbox)

        TransferFunctionEditor(tfContainer = stageSpaceManager)
        //SettingsEditor(MicroscenerySettings)
        stageSpaceManager.minDisplayRange = 50f


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

        inputHandler?.let {
            stageUserInteraction(stageSpaceManager, it, cam)
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            LocalMMScene().main()
        }
    }

}