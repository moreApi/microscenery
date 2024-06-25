package microscenery.example

import bdv.util.AxisOrder
import bvv.core.VolumeViewerOptions
import graphics.scenery.volumes.Colormap
import graphics.scenery.volumes.TransferFunction
import graphics.scenery.volumes.Volume
import ij.IJ
import ij.ImagePlus
import microscenery.DefaultVRScene
import microscenery.MicrosceneryHub
import microscenery.VRUI.VRUIManager
import microscenery.VRUI.fromScenery.WheelMenu
import net.imglib2.img.Img
import net.imglib2.img.display.imagej.ImageJFunctions
import net.imglib2.type.numeric.integer.UnsignedShortType
import org.joml.Vector3f

/**
 * VR, mmConnection local or controlled Stream remote
 */
class T1HeadVR : DefaultVRScene(T1HeadVR::class.java.simpleName) {


    override fun init() {
        super.init()

        cam.spatial().position = Vector3f(0f, -5f, 5f)

        val imp: ImagePlus = IJ.openImage("""E:\volumes\t1-head.tif""")
        val img: Img<UnsignedShortType> = ImageJFunctions.wrapShort(imp)

        val volume = Volume.fromRAI(img, UnsignedShortType(), AxisOrder.DEFAULT, "T1 head", hub, VolumeViewerOptions())

//        volume.spatial().scale= Vector3f(0.1f,0.1f,0.4f)
        volume.colormap = Colormap.get("plasma")
        volume.transferFunction = TransferFunction.ramp(0.0017f, 1f, 0.5f)
        scene.addChild(volume)

    }

    override fun inputSetup() {
        super.inputSetup()

        VRUIManager.initBehavior(
            scene, hmd, inputHandler, WheelMenu(
                hmd, listOf(

                )
            ), msHub = MicrosceneryHub(hub)
        )
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            T1HeadVR().main()
        }
    }
}