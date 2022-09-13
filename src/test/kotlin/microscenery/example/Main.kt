package microscenery.example

import microscenery.MicroscenerySettings
import graphics.scenery.controls.behaviours.Action
import graphics.scenery.controls.behaviours.Switch
import graphics.scenery.controls.behaviours.WheelMenu
import graphics.scenery.utils.extensions.times
import graphics.scenery.volumes.Colormap
import graphics.scenery.volumes.TransferFunction
import graphics.scenery.volumes.Volume
import microscenery.ControlledVolumeStreamClient
import microscenery.DefaultVRScene
import microscenery.MMConnection
import microscenery.StreamedVolume
import microscenery.UI.DisplayRangeEditor
import microscenery.VRUI.VRUIManager
import org.joml.Vector3f
import kotlin.concurrent.thread

/**
 * VR, mmConnection local or controlled Stream remote
 */
class Main : DefaultVRScene(Main::class.java.simpleName) {

    val cvsc = ControlledVolumeStreamClient(scene, hub)

    override fun init() {
        prepareVRScene()

        cam.spatial().position = Vector3f(0f, 0f, 5f)


        if (MicroscenerySettings.get("Main.remote", false)) {
            cvsc.init()
            val lastUpdateBoard = cvsc.lastAcquisitionTextBoard()
            lastUpdateBoard.spatial {
                position = Vector3f(0f, 9f, -9f)
                scale = Vector3f(1f)
                //rotation = Quaternionf().rotationX(-PI.toFloat())
            }
            scene.addChild(lastUpdateBoard)
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

        thread {
            while (cvsc.mmVol == null) {
                Thread.sleep(200)
            }
            cvsc.mmVol?.let {
                //it.volume.spatial().scale= Vector3f(0.225f,0.225f,3.348f) * 0.3f
                it.volume.spatial().scale= Vector3f(0.225f,0.225f,1.524f) * 0.3f
                //it.volume.spatial().scale= Vector3f(0.1125f,0.1125f,1.524f) * 0.3f
                it.volume.transferFunction = TransferFunction.ramp(0f,0.8f,1f)
                it.volume.converterSetups.first().setDisplayRange(200.0,860.0)
                //it.volume.transferFunction = TransferFunction.ramp(0.002925f,1f,0.25f)
            //it.volume.transferFunction = TransferFunction.ramp(0.002934f,1f,0.01f)

                DisplayRangeEditor(it.volume.converterSetups.first()).isVisible = true
            }
        }
    }

    override fun inputSetup() {
        super.inputSetup()

        VRUIManager.initBehavior(scene, hmd, inputHandler, WheelMenu(hmd, listOf(Switch(
            "imaging", false
        ) {
            if (it) {
                cvsc.start()
            } else {
                cvsc.pause()
            }
        }, Action("snap", false) { cvsc.snap() }))) {
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