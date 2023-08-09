package microscenery.example

import graphics.scenery.controls.behaviours.WheelMenu
import graphics.scenery.volumes.Colormap
import graphics.scenery.volumes.TransferFunction
import graphics.scenery.volumes.Volume
import microscenery.DefaultVRScene
import microscenery.VRUI.ModifiableBox
import microscenery.VRUI.VRUIManager
import kotlin.io.path.Path

/**
 * VR, mmConnection local or controlled Stream remote
 */
class T1HeadVR : DefaultVRScene(T1HeadVR::class.java.simpleName) {


    override fun init() {
        prepareVRScene()

        //cam.spatial().position = Vector3f(0f, -5f, 5f)


        val volume = Volume.fromPath(Path("""C:\Users\JanCasus\volumes\t1-head.tif"""), hub)
        scene.addChild(volume)
//        volume.spatial().scale= Vector3f(0.1f,0.1f,0.4f)
        volume.colormap = Colormap.get("plasma")
        volume.transferFunction = TransferFunction.ramp(0.0017f, 1f, 0.01f)

        scene.addChild(ModifiableBox())

    }

    override fun inputSetup() {
        super.inputSetup()

        VRUIManager.initBehavior(
            scene, hmd, inputHandler, WheelMenu(
                hmd, listOf(

                )
            )
        ) {
            scene.findByClassname(Volume::class.simpleName!!).first() as Volume
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            T1HeadVR().main()
        }
    }
}