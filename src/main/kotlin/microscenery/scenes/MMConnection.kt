package microscenery.scenes

import graphics.scenery.*
import graphics.scenery.backends.Renderer
import microscenery.MMConnectedVolume
import org.joml.Vector3f

class MMConnection : SceneryBase(MMConnection::class.java.simpleName, windowWidth = 1920, windowHeight = 1200) {

    val slices = 30 // has to be 0 != x%32 otherwise we have a standing wave

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

        val mmVol = MMConnectedVolume(hub, slices)
        scene.addChild(mmVol.volume)

    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            MMConnection().main()
        }
    }
}