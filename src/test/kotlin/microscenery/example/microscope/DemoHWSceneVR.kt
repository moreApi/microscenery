package microscenery.example.microscope

import graphics.scenery.Sphere
import graphics.scenery.controls.behaviours.Grabable
import graphics.scenery.controls.behaviours.Touchable
import graphics.scenery.volumes.Volume
import microscenery.DefaultVRScene
import microscenery.StageSpaceManager
import microscenery.VRUI.VRUIManager
import microscenery.hardware.DemoMicroscopeHardware
import org.joml.Vector3f
import kotlin.concurrent.thread

class DemoHWSceneVR : DefaultVRScene() {

    override fun init() {
        super.init()


        val hw = DemoMicroscopeHardware()
        val stageSpaceManager = StageSpaceManager(hw, scene,hub, addFocusFrame = true, scaleDownFactor = 200f)

        Sphere(0.5f).apply {
            spatial {
                scale = Vector3f(10f)
            }
            addAttribute(Grabable::class.java, Grabable())
            addAttribute(Touchable::class.java, Touchable())
            stageSpaceManager.stageRoot.addChild(this)
        }

        thread {
            while (true) {
                Thread.sleep(200)
                scene
            }
        }
    }

    override fun inputSetup() {
        super.inputSetup()

        VRUIManager.initBehavior(scene, hmd, inputHandler) {
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