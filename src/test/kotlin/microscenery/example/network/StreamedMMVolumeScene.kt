package microscenery.example.network

import microscenery.MicroscenerySettings
import graphics.scenery.*
import graphics.scenery.backends.Renderer
import microscenery.StreamedVolume
import microscenery.network.VolumeReceiver
import org.joml.Vector3f

class StreamedMMVolumeScene : SceneryBase(
    StreamedMMVolumeScene::class.java.simpleName,
    windowWidth = 1920,
    windowHeight = 1200,
    wantREPL = false
) {

    lateinit var mmVol: StreamedVolume

    val slices = MicroscenerySettings.get<Int>("MMConnection.slices")

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
            perspectiveCamera(50.0f, width, height)

            scene.addChild(this)
        }

        val width = 700
        val height = 660

        val connection = VolumeReceiver(
            reuseBuffers = false, zContext = zContext, width * height * slices * Short.SIZE_BYTES
        )
        var time = 0L
        val timeBetweenUpdates = 1000
        mmVol = StreamedVolume(hub, width, height, slices) {
            //wait at least timeBetweenUpdates
            (System.currentTimeMillis() - time).let {
                if (it in 1..timeBetweenUpdates)
                    Thread.sleep(timeBetweenUpdates - it)
            }
            time = System.currentTimeMillis()
            connection.getVolume(2000, it)
        }
        scene.addChild(mmVol.volume)

    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            StreamedMMVolumeScene().main()
        }
    }
}