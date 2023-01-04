package microscenery.example.microscope

import graphics.scenery.controls.behaviours.*
import graphics.scenery.volumes.Volume
import microscenery.DefaultVRScene
import microscenery.stageSpace.StageSpaceManager
import microscenery.VRUI.VRUIManager
import microscenery.hardware.DemoMicroscopeHardware
import microscenery.stageSpace.FocusFrame
import kotlin.concurrent.thread

class DemoHWSceneVR : DefaultVRScene() {

    lateinit var stageSpaceManager: StageSpaceManager

    override fun init() {
        super.init()


        val hw = DemoMicroscopeHardware()
        stageSpaceManager = StageSpaceManager(hw, scene, hub, addFocusFrame = true, scaleDownFactor = 200f)


        thread {
            while (true) {
                Thread.sleep(200)
                scene
            }
        }
    }

    override fun inputSetup() {
        super.inputSetup()

        VRUIManager.initBehavior(scene, hmd, inputHandler,
        customActions = WheelMenu(hmd, listOf(
            Switch("Steering", false) { value ->
                stageSpaceManager.focusTarget?.let {
                    if (value && it.mode != FocusFrame.Mode.STEERING) {
                        it.mode = FocusFrame.Mode.STEERING
                    } else {
                        it.mode = FocusFrame.Mode.PASSIVE
                    }
                    logger.info("focusframe mode is now ${it.mode}")
                }
            },
            Switch("Live", false) { stageSpaceManager.hardware.live = it },
        ))
        ) {
            scene.findByClassname(Volume::class.simpleName!!).first() as Volume
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            DemoHWSceneVR().main()
        }
    }

}