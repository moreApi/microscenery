package microscenery.example.microscope

import graphics.scenery.AmbientLight
import microscenery.DefaultVRScene
import microscenery.MicrosceneryHub
import microscenery.MicroscenerySettings
import microscenery.UI.StageSpaceUI
import microscenery.VRUI.VRUIManager
import microscenery.hardware.MicroscopeHardware
import microscenery.hardware.micromanagerConnection.MMCoreConnector
import microscenery.hardware.micromanagerConnection.MicromanagerWrapper
import microscenery.stageSpace.MicroscopeLayout
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector3f

class LocalMMSceneVR : DefaultVRScene() {

    lateinit var stageSpaceManager: StageSpaceManager

    override fun init() {
        super.init()

        MicroscenerySettings.set("Stage.minX", -100f)
        MicroscenerySettings.set("Stage.minY", -100f)
        MicroscenerySettings.set("Stage.minZ", -50f)
        MicroscenerySettings.set("Stage.maxX", 500f)
        MicroscenerySettings.set("Stage.maxY", 500f)
        MicroscenerySettings.set("Stage.maxZ", 250f)

        val stageStart = Vector3f()

        val hardware: MicroscopeHardware =
            MicromanagerWrapper(MMCoreConnector(LocalMMScene.initLocalMMCoreFake()).apply { moveStage(stageStart, false) })
        val msHub = MicrosceneryHub(hub)
        stageSpaceManager = StageSpaceManager(
            hardware, scene, msHub, layout = MicroscopeLayout.Default(MicroscopeLayout.Axis.Z)
        )

        hullbox.visible = false

        scene.addChild(AmbientLight(0.25f))

    }

    override fun inputSetup() {
        super.inputSetup()
        val ssUI = StageSpaceUI(stageSpaceManager)

        inputHandler?.let {
            ssUI.stageKeyUI(it, cam)
        }

        VRUIManager.initBehavior(
            scene, hmd, inputHandler,
            stageSpaceUI = ssUI, msHub = MicrosceneryHub(hub)
        )
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            LocalMMSceneVR().main()
        }
    }

}