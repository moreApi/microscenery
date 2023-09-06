package microscenery.example.microscope

import microscenery.*
import microscenery.UI.StageSpaceUI
import microscenery.VRUI.VRUIManager
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector3f
import kotlin.concurrent.thread

class DemoHWSceneVR : DefaultVRScene() {

    lateinit var stageSpaceManager: StageSpaceManager

    override fun init() {
        super.init()

        MicroscenerySettings.set("Stage.precisionXY", 1f)
        MicroscenerySettings.set("Stage.precisionZ", 1f)
        MicroscenerySettings.setVector3f("Ablation.PointCloud.MinDistUm", Vector3f(1f))

        MicroscenerySettings.set(Settings.Ablation.Enabled, true)
        MicroscenerySettings.set(Settings.VRToolbox.PointAblationEnabled, true)
        MicroscenerySettings.set(Settings.VRToolbox.PathAblationEnabled, true)

        val hw = DemoMicroscopeHardware(dataSide = 100)
        stageSpaceManager = StageSpaceManager(hw, scene, hub, addFocusFrame = true)


        thread {
            while (true) {
                Thread.sleep(200)
                scene
            }
        }
    }

    override fun inputSetup() {
        super.inputSetup()
        val ssUI = StageSpaceUI(stageSpaceManager)

        inputHandler?.let {
            ssUI.stageKeyUI(it, cam)
        }

        VRUIManager.initBehavior(
            scene, hmd, inputHandler,
            stageSpaceUI = ssUI
        ) {
            stageSpaceManager.stageRoot
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            DemoHWSceneVR().main()
        }
    }

}