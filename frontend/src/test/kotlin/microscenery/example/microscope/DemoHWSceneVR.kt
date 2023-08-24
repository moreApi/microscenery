package microscenery.example.microscope

import microscenery.DefaultVRScene
import microscenery.DemoMicroscopeHardware
import microscenery.MicroscenerySettings
import microscenery.UI.StageSpaceUI
import microscenery.VRUI.VRUIManager
import microscenery.setVector3
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector3f
import kotlin.concurrent.thread

class DemoHWSceneVR : DefaultVRScene() {

    lateinit var stageSpaceManager: StageSpaceManager

    override fun init() {
        super.init()

        MicroscenerySettings.set("Stage.precisionXY", 1f)
        MicroscenerySettings.set("Stage.precisionZ", 1f)
        MicroscenerySettings.setVector3("Ablation.PointCloud.MinDistUm", Vector3f(1f))

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