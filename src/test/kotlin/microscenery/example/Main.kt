package microscenery.example

import graphics.scenery.controls.behaviours.Switch
import graphics.scenery.controls.behaviours.WheelMenu
import graphics.scenery.volumes.Volume
import microscenery.ControlledVolumeStreamClient
import microscenery.DefaultVRScene
import microscenery.VRUI.VRUIManager

class Main : DefaultVRScene(Main::class.java.simpleName) {

    val cvsc = ControlledVolumeStreamClient(scene, hub)

    override fun init() {
        prepareVRScene()
        cvsc.init()

        /*val mmConnection = MMConnection()
        val mmConnectionVolume = StreamedVolume(
            hub,
            mmConnection.width,
            mmConnection.height,
            mmConnection.steps
        ) { mmConnection.captureStack(it.asShortBuffer()) }
        volume = mmConnectionVolume.volume
        scene.addChild(volume)

         */
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
                    }
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