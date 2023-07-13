package microscenery.example.microscope

import microscenery.DefaultVRScene
import microscenery.DemoMicroscopeHardware
import microscenery.UI.StageSpaceUI
import microscenery.VRUI.VRUIManager
import microscenery.stageSpace.StageSpaceManager
import kotlin.concurrent.thread

class DemoHWSceneVR : DefaultVRScene() {

    lateinit var stageSpaceManager: StageSpaceManager

    override fun init() {
        super.init()


        val hw = DemoMicroscopeHardware()
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