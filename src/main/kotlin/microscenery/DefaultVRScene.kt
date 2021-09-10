package microscenery

import graphics.scenery.*
import graphics.scenery.attribute.material.Material
import graphics.scenery.backends.Renderer
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackedDeviceType
import graphics.scenery.numerics.Random
import org.joml.Vector3f
import kotlin.concurrent.thread
import kotlin.system.exitProcess

/**
 * Default VR Scene setup provided by [prepareVRScene] method
 */
abstract class DefaultVRScene(name: String) : SceneryBase(
    name,
    windowWidth = 1920, windowHeight = 1200
) {
    protected lateinit var hmd: OpenVRHMD
    protected lateinit var hullbox: Box

    protected fun prepareVRScene() {
        hmd = OpenVRHMD(useCompositor = true)

        if (!hmd.initializedAndWorking()) {
            logger.error("This demo is intended to show the use of OpenVR, but no OpenVR-compatible HMD could be initialized.")
            exitProcess(1)
        }
        hub.add(SceneryElement.HMDInput, hmd)

        renderer = hub.add(Renderer.createRenderer(hub, applicationName, scene, windowWidth, windowHeight))
        renderer?.toggleVR()

        val cam: Camera = DetachedHeadCamera(hmd)
        cam.spatial {
            position = Vector3f(0.0f, 0.0f, 0.0f)
        }
        cam.perspectiveCamera(50.0f, windowWidth, windowHeight)
        scene.addChild(cam)

        thread {
            while (!running) {
                Thread.sleep(200)
            }

            hmd.events.onDeviceConnect.add { hmd, device, timestamp ->
                if (device.type == TrackedDeviceType.Controller) {
                    logger.info("Got device ${device.name} at $timestamp")
                    device.model?.let { controller ->
                        // This attaches the model of the controller to the controller's transforms
                        // from the OpenVR/SteamVR system.
                        hmd.attachToNode(device, controller, cam)
                    }
                }
            }
        }

        (0..5).map {
            val light = PointLight(radius = 15.0f)
            light.emissionColor = Random.random3DVectorFromRange(0.5f, 1.0f)
            light.spatial {
                position = Random.random3DVectorFromRange(-5.0f, 5.0f)
            }
            light.intensity = 1.0f

            light
        }.forEach { scene.addChild(it) }

        hullbox = Box(Vector3f(20.0f, 20.0f, 20.0f), insideNormals = true)
        hullbox.material {
            ambient = Vector3f(0.6f, 0.6f, 0.6f)
            diffuse = Vector3f(0.4f, 0.4f, 0.4f)
            specular = Vector3f(0.0f, 0.0f, 0.0f)
            cullingMode = Material.CullingMode.Front
        }
        scene.addChild(hullbox)
    }
}