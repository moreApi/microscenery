package microscenery.example

import MicroscenerySettings
import graphics.scenery.controls.behaviours.Action
import graphics.scenery.controls.behaviours.Switch
import graphics.scenery.controls.behaviours.WheelMenu
import graphics.scenery.volumes.Colormap
import graphics.scenery.volumes.TransferFunction
import graphics.scenery.volumes.Volume
import microscenery.ControlledVolumeStreamClient
import microscenery.DefaultVRScene
import microscenery.MMConnection
import microscenery.StreamedVolume
import microscenery.VRUI.VRUIManager
import org.joml.Vector3f

/**
 * VR, mmConnection local or controlled Stream remote
 */
class Main : DefaultVRScene(Main::class.java.simpleName) {

    val cvsc = ControlledVolumeStreamClient(scene, hub)

    override fun init() {
        prepareVRScene()

        cam.spatial().position = Vector3f(0f,-5f,5f)


        if (MicroscenerySettings.get("Main.remote",false)){
            cvsc.init()
        } else {
            val mmConnection = MMConnection()
            val mmConnectionVolume = StreamedVolume(
                hub,
                mmConnection.width,
                mmConnection.height,
                mmConnection.steps
            ) { mmConnection.captureStack(it.asShortBuffer())
                it}
            val volume = mmConnectionVolume.volume
            scene.addChild(volume)
            volume.spatial().scale= Vector3f(0.1f,0.1f,0.4f)
            volume.colormap = Colormap.get("plasma")
            volume.transferFunction = TransferFunction.ramp(0.0017f,1f,0.01f)
        }
    }

    override fun inputSetup() {
        super.inputSetup()

        VRUIManager.initBehavior(
            scene, hmd, inputHandler, WheelMenu(
                hmd, listOf(
                    Switch("imaging", false
                    ) {
                        if (it) {
                            cvsc.start()
                        } else {
                            cvsc.pause()
                        }
                    },
                    Action("snap", false){cvsc.snap()}
                )
            )
        ) {
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