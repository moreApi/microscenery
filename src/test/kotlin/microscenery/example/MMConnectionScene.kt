package microscenery.example

import graphics.scenery.*
import graphics.scenery.backends.Renderer
import graphics.scenery.volumes.Colormap
import graphics.scenery.volumes.TransferFunction
import microscenery.MMConnection
import microscenery.StreamedVolume
import org.joml.Vector3f
import kotlin.concurrent.thread

/**
 * local mmConnection, no VR
 */
class MMConnectionScene :
    SceneryBase(MMConnectionScene::class.java.simpleName, windowWidth = 1920, windowHeight = 1200) {

    lateinit var mmVol: StreamedVolume

    override fun init() {
        renderer = hub.add(
            SceneryElement.Renderer,
            Renderer.createRenderer(hub, applicationName, scene, 512, 512)
        )

        val light = PointLight(radius = 15.0f)
        light.spatial().position = Vector3f(0.0f, 0.0f, 2.0f)
        light.intensity = 5.0f
        light.emissionColor = Vector3f(1.0f, 1.0f, 1.0f)
        scene.addChild(light)

        val cam: Camera = DetachedHeadCamera()
        with(cam) {
            spatial {
                position = Vector3f(0.0f, 0.0f, 15.0f)
            }
            perspectiveCamera(50.0f, 512, 512)

            scene.addChild(this)
        }



        val mmConnection = MMConnection()
        mmVol = StreamedVolume(
            hub,
            mmConnection.width,
            mmConnection.height,
            mmConnection.steps
        ) {
            mmConnection.captureStack(it.asShortBuffer())
            it
        }
        scene.addChild(mmVol.volume)
        mmVol.volume.spatial().scale= Vector3f(0.1f,0.1f,0.4f)
        mmVol.volume.colormap = Colormap.get("plasma")
        mmVol.volume.transferFunction = TransferFunction.ramp(0.0017f,1f,0.01f)

        thread {
            while (true){
                Thread.sleep(200)
                print(scene)
            }
        }

    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            MMConnectionScene().main()
        }
    }
}