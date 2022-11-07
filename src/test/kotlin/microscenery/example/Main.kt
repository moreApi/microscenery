package microscenery.example

import graphics.scenery.volumes.Volume
import microscenery.DefaultVRScene
import microscenery.VRUI.VRUIManager
import org.joml.Vector3f

/**
 * VR, mmConnection local or controlled Stream remote
 */
class Main : DefaultVRScene(Main::class.java.simpleName) {


    override fun init() {
        prepareVRScene()

        cam.spatial().position = Vector3f(0f, 0f, 5f)


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
            Main().main()
        }
    }
}