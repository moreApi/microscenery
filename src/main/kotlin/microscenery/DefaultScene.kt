package microscenery

import graphics.scenery.*
import graphics.scenery.backends.Renderer
import org.joml.Vector3f

class DefaultScene(val initHook: ((scene:Scene,hub:Hub)-> Unit)?, name: String = "Microscenery"):SceneryBase(name, wantREPL = false) {
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
                position = Vector3f(0.0f, 0.0f, 5.0f)
            }
            perspectiveCamera(50.0f, 512, 512)

            scene.addChild(this)
        }

        initHook?.invoke(scene,hub)



    }
}