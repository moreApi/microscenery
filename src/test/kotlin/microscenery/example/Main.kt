package microscenery.example

import microscenery.MicroscenerySettings
import graphics.scenery.volumes.Colormap
import graphics.scenery.volumes.TransferFunction
import graphics.scenery.volumes.Volume
import microscenery.DefaultVRScene
import microscenery.hardware.micromanagerConnection.MMConnection
import microscenery.StreamedVolume
import microscenery.VRUI.VRUIManager
import org.joml.Vector3f

/**
 * VR, mmConnection local or controlled Stream remote
 */
class Main : DefaultVRScene(Main::class.java.simpleName) {


    override fun init() {
        prepareVRScene()

        cam.spatial().position = Vector3f(0f, 0f, 5f)


        if (MicroscenerySettings.get("Main.remote", false)) {

        } else {
            val mmConnection = MMConnection()
            val mmConnectionVolume = StreamedVolume(
                hub, mmConnection.width, mmConnection.height, 5//mmConnection.steps
            ) {
                //mmConnection.captureStack(it.asShortBuffer())
                it
            }
            val volume = mmConnectionVolume.volume
            scene.addChild(volume)
            volume.spatial().scale = Vector3f(0.1f, 0.1f, 0.4f)
            volume.colormap = Colormap.get("plasma")
            volume.transferFunction = TransferFunction.ramp(0.0017f, 1f, 0.01f)
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
            Main().main()
        }
    }
}