package microscenery.example.network

import graphics.scenery.*
import graphics.scenery.backends.Renderer
import lightSleepOn
import microscenery.ControlledVolumeStreamClient
import org.joml.Vector3f
import kotlin.concurrent.thread

class ControlledVolumeStreamClientScene : SceneryBase(
    ControlledVolumeStreamClientScene::class.java.simpleName, windowWidth = 1920, windowHeight = 1200, wantREPL = true
) {

    val cvsc = ControlledVolumeStreamClient(scene, hub)

    override fun init() {
        baseInit()
        cvsc.init()

    }


    private fun baseInit() {
        renderer = hub.add(
            SceneryElement.Renderer, Renderer.createRenderer(hub, applicationName, scene, 512, 512)
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
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val b = ControlledVolumeStreamClientScene()
            thread {
                lightSleepOn(15000) { b.cvsc.latestServerStatus }
                println("status here")
                b.cvsc.start()
            }
            b.main()
        }
    }
}