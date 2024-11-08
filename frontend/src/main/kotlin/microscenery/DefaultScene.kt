package microscenery

import graphics.scenery.*
import graphics.scenery.attribute.material.Material
import graphics.scenery.backends.Renderer
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackedDeviceType
import graphics.scenery.numerics.Random
import graphics.scenery.utils.extensions.times
import microscenery.simulation.StageSimulation.Companion.hideMaterial
import net.miginfocom.swing.MigLayout
import org.joml.Vector3f
import org.scijava.ui.behaviour.InputTrigger
import java.awt.BorderLayout
import java.awt.Point
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.system.exitProcess

abstract class DefaultScene(
    name: String = "Microscenery",
    width: Int = 600, height: Int = 600,
    val withSwingUI: Boolean = false,
    val VR: Boolean = false
) : SceneryBase(name, wantREPL = false, windowWidth = if (VR) 1920 else width, windowHeight = if (VR) 1080 else height) {
    var mainFrame: JFrame? = null
    var extraPanel: JPanel? = null

    lateinit var cam: Camera
    protected lateinit var hmd: InputManagerVRHMD
    protected lateinit var hullbox: Box

    override fun init() {

        renderer = hub.add(
            SceneryElement.Renderer,
            Renderer.createRenderer(hub, applicationName, scene, windowWidth, windowHeight)
        )
        if (VR) {
            hmd = InputManagerVRHMD()

            if (!hmd.initializedAndWorking()) {
                logger.error("This is intended to use OpenVR, but no OpenVR-compatible HMD could be initialized.")
                exitProcess(1)
            }
            hub.add(SceneryElement.HMDInput, hmd)

            renderer?.toggleVR()

            cam = DetachedHeadCamera(hmd).apply {
                spatial {
                    position = Vector3f(0.0f, 0.0f, 1.5f)
                }
                perspectiveCamera(50.0f, windowWidth, windowHeight)
                scene.addChild(this)
            }

            hmd.events.onDeviceConnect.add { hmd, device, timestamp ->
                if (device.type == TrackedDeviceType.Controller) {
                    logger.info("Got device ${device.name} at $timestamp")
                    device.model?.let { controller ->
                        // This attaches the model of the controller to the controller's transforms
                        // from the OpenVR/SteamVR system.
                        hmd.attachToNode(device, controller, cam)

                        //Create aim balls on top of controllers
                        val indicator = Sphere(0.015f, 10)
                        //TODO  POSTSTUDY reactivate
                        indicator.hideMaterial()
                        indicator.name = "indicator"
                        indicator.material().diffuse = Vector3f(1f)
                        controller.addChild(indicator)

                        val collider = Box(Vector3f(.01f))
                        //TODO  POSTSTUDY reactivate
                        collider.hideMaterial()
                        collider.name = "collider"
                        controller.addChild(collider)
                    }
                }
            }
        } else {

            cam = DetachedHeadCamera().apply {
                spatial {
                    position = Vector3f(0.0f, 0.0f, 1.5f)
                }
                perspectiveCamera(50.0f, windowWidth, windowHeight)
                scene.addChild(this)
            }
        }

        if (MicroscenerySettings.getOrNull(Settings.StageSpace.ShowHullbox) ?: VR){
            hullbox = Box(Vector3f(10.0f, 10.0f, 10.0f), insideNormals = true)
            hullbox.name = "hullbox"
            hullbox.material {
                ambient = Vector3f(0.6f, 0.6f, 0.6f)
                diffuse = Vector3f(0.4f, 0.4f, 0.4f)
                specular = Vector3f(0.0f, 0.0f, 0.0f)
                cullingMode = Material.CullingMode.Front
            }
            hullbox.spatial().position = Vector3f(0f, 2.5f, 0f)
            scene.addChild(hullbox)
        }

        if (withSwingUI) {
            mainFrame = JFrame("$applicationName Controls")
            mainFrame?.layout = BorderLayout()

            mainFrame?.isVisible = true

            extraPanel = JPanel(MigLayout())
            mainFrame?.add(extraPanel!!)
            mainFrame?.pack()
            mainFrame?.location = Point((windowWidth * 1.2).toInt(), 50)
        }
        initLight()
    }

    private fun initLight() {
        if (VR) {
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

            scene.addChild(AmbientLight(intensity = 0.1f))
        } else {

            val light = PointLight(radius = 15.0f)
            light.spatial().position = Vector3f(2.0f, 1.0f, 2.0f) * 2f
            light.intensity = 15.0f
            light.emissionColor = Vector3f(1.0f, 1.0f, 1.0f)
            scene.addChild(light)

            val light2 = PointLight(radius = 15.0f)
            light2.spatial().position = Vector3f(-2.0f, -1.0f, -2.0f) * 2f
            light2.intensity = 15.0f
            light2.emissionColor = Vector3f(1.0f, 1.0f, 1.0f)
            scene.addChild(light2)
        }
    }

    override fun close() {
        mainFrame?.dispose()
        super.close()
    }
}

/**
 * Default VR Scene setup
 */
abstract class DefaultVRScene(
    name: String = "VR Scene", withSwingUI: Boolean = false
) : DefaultScene(
    name, VR = true, withSwingUI = withSwingUI
)

class InputManagerVRHMD : OpenVRHMD(){
    /**
     * Returns *a copy of* all the currently set key bindings
     *
     * @return Map of all currently configured key bindings.
     */
    fun getAllBindings(): Map<InputTrigger, Set<String>> {
        return inputMap.allBindings
    }

    /**
     * Removes a key binding for a given behaviour
     *
     * @param[behaviourName] The behaviour to remove the key binding for.
     */
    fun removeKeyBinding2(behaviourName: String) {
        config.getInputs(behaviourName, "all").forEach { inputTrigger ->
            inputMap.remove(inputTrigger, behaviourName)
            config.remove(inputTrigger, behaviourName, "all")
        }
    }
}