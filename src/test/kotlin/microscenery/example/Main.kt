package microscenery.example

import graphics.scenery.volumes.Volume
import microscenery.DefaultVRScene
import microscenery.MMConnection
import microscenery.StreamedVolume
import microscenery.VRUI.VRUIManager

class Main : DefaultVRScene(Main::class.java.simpleName) {
    private lateinit var volume: Volume

    override fun init() {
        prepareVRScene()

        val mmConnection = MMConnection()
        val mmConnectionVolume = StreamedVolume(
            hub,
            mmConnection.width,
            mmConnection.height,
            mmConnection.slices
        ) { mmConnection.captureStack(it.asShortBuffer()) }
        volume = mmConnectionVolume.volume
        scene.addChild(volume)
    }

    override fun inputSetup() {
        super.inputSetup()

        VRUIManager.initBehavior(scene, hmd, inputHandler, volume)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Main().main()
        }
    }
}