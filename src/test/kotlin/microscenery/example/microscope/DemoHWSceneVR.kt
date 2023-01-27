package microscenery.example.microscope

import graphics.scenery.controls.behaviours.Switch
import graphics.scenery.controls.behaviours.WheelMenu
import graphics.scenery.volumes.Volume
import microscenery.DefaultVRScene
import microscenery.DemoMicroscopeHardware
import microscenery.VRUI.VRUIManager
import microscenery.stageSpace.FrameGizmo
import microscenery.stageSpace.StageSpaceManager
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

        VRUIManager.initBehavior(
            scene, hmd, inputHandler,
            customActions = WheelMenu(
                hmd, listOf(
                    Switch("Steering", false) { value ->
                        stageSpaceManager.focusTarget?.let {
                            if (value && it.mode != FrameGizmo.Mode.STEERING) {
                                it.mode = FrameGizmo.Mode.STEERING
                            } else {
                                it.mode = FrameGizmo.Mode.PASSIVE
                            }
                            logger.info("focusframe mode is now ${it.mode}")
                        }
                    },
                    Switch("Live", false) {
                        if (it)
                            stageSpaceManager.goLive()
                        else
                            stageSpaceManager.stop()
                    },
                )
            )
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