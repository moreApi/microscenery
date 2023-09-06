package microscenery

import graphics.scenery.*
import graphics.scenery.attribute.material.Material
import graphics.scenery.backends.Renderer
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackedDeviceType
import graphics.scenery.numerics.Random
import org.joml.Vector3f
import kotlin.system.exitProcess

/**
 * Default VR Scene setup provided by [prepareVRScene] method
 */
abstract class DefaultVRScene(name: String = "VR Scene") : SceneryBase(
    name,
    windowWidth = 1920, windowHeight = 1080,
    wantREPL = false
) {
    protected lateinit var cam: Camera
    protected lateinit var hmd: OpenVRHMD
    protected lateinit var hullbox: Box

    /**
     * has to be called in init
     */
    protected fun prepareVRScene() {
        hmd = OpenVRHMD(useCompositor = true)

        if (!hmd.initializedAndWorking()) {
            logger.error("This is intended to use OpenVR, but no OpenVR-compatible HMD could be initialized.")
            exitProcess(1)
        }
        hub.add(SceneryElement.HMDInput, hmd)

        renderer = hub.add(Renderer.createRenderer(hub, applicationName, scene, windowWidth, windowHeight))
        renderer?.toggleVR()

        cam = DetachedHeadCamera(hmd)
        cam.spatial {
            position = Vector3f(0.0f, 0.0f, 5.0f)
        }
        cam.perspectiveCamera(50.0f, windowWidth, windowHeight)
        scene.addChild(cam)

        hmd.events.onDeviceConnect.add { hmd, device, timestamp ->
            if (device.type == TrackedDeviceType.Controller) {
                logger.info("Got device ${device.name} at $timestamp")
                device.model?.let { controller ->
                    // This attaches the model of the controller to the controller's transforms
                    // from the OpenVR/SteamVR system.
                    hmd.attachToNode(device, controller, cam)

                    //Create aim balls on top of controllers
                    val indicator = Sphere(0.015f, 10)
                    indicator.name = "indicator"
                    indicator.material().diffuse = Vector3f(1f)
                    controller.addChild(indicator)

                    val collider = Box(Vector3f(.00001f))
                    collider.name = "collider"
                    controller.addChild(collider)
                }
            }
        }

        val lightPositions = listOf(
            Vector3f(3f, 3f, 3f),
            Vector3f(3f, 3f, -3f),
            Vector3f(-3f, 3f, 3f),
            Vector3f(-3f, 3f, -3f),
            Vector3f(0f, -3f, 0f),
        )

        lightPositions.map {
            val light = PointLight(radius = 15.0f)
            light.emissionColor = Random.random3DVectorFromRange(0.75f, 1.0f)
            light.spatial {
                position = it
            }
            light.intensity = 1.0f

            light
        }.forEach { scene.addChild(it) }

        scene.addChild(AmbientLight(intensity = 0.05f))

        hullbox = Box(Vector3f(20.0f, 20.0f, 20.0f), insideNormals = true)
        hullbox.name = "hullbox"
        hullbox.material {
            ambient = Vector3f(0.6f, 0.6f, 0.6f)
            diffuse = Vector3f(0.4f, 0.4f, 0.4f)
            specular = Vector3f(0.0f, 0.0f, 0.0f)
            cullingMode = Material.CullingMode.Front
        }
        scene.addChild(hullbox)
    }


    override fun init() {
        super.init()
        prepareVRScene()
    }
}