package microscenery.VRUI.behaviors

import graphics.scenery.Node
import graphics.scenery.Scene
import graphics.scenery.Sphere
import graphics.scenery.attribute.spatial.Spatial
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackedDeviceType
import graphics.scenery.controls.TrackerInput
import graphics.scenery.controls.TrackerRole
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plus
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.joml.Vector3f
import org.scijava.ui.behaviour.DragBehaviour
import microscenery.wrapForAnalogInputIfNeeded
import kotlin.math.pow

/**
 * Go-Go Teleport
 */
class VRTeleport(
    protected val name: String,
    protected val controllerHitbox: Node,
    private val cam: Spatial,
    private val hmd: OpenVRHMD
) : DragBehaviour {

    val controllerSpatial: Spatial = controllerHitbox.spatialOrNull()
        ?: throw IllegalArgumentException("controller hitbox needs a spatial attribute")

    val target = Sphere(0.15f)

    init {
        target.material().diffuse = Vector3f(0f, 1.0f, 0f)
    }

    override fun init(x: Int, y: Int) {
        controllerHitbox.addChild(target)
    }

    override fun drag(x: Int, y: Int) {
        val bodyCenter = hmd.getPosition().mul(0.5f) + cam.worldPosition()
        bodyCenter.y = 0f
        val controller = controllerSpatial.worldPosition()
        controller.y = 0f
        val dist = bodyCenter.distance(controller) * 2
        target.spatial().position.z = dist.pow(3) * -1f
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun end(x: Int, y: Int) {
        GlobalScope.launch {
            val fadeTime = 300L
            hmd.fadeToBlack(fadeTime * 0.001f)
            delay(fadeTime)
            cam.position = target.spatial().worldPosition() - hmd.getPosition().mul(0.5f)
            hmd.fateToClear(fadeTime * 0.001f)
        }
        controllerHitbox.removeChild(target)
    }

    companion object {

        /**
         * Convenience method for adding grab behaviour
         */
        fun createAndSet(
            scene: Scene,
            hmd: OpenVRHMD,
            buttons: List<OpenVRHMD.OpenVRButton>,
            controllerSide: List<TrackerRole>
        ) {
            hmd.events.onDeviceConnect.add { _, device, _ ->
                if (device.type == TrackedDeviceType.Controller) {
                    device.model?.let { controller ->
                        if (controllerSide.contains(device.role)) {
                            buttons.forEach { button ->
                                val name = "VRTeleport:${hmd.trackingSystemName}:${device.role}:$button"
                                val behaviour = VRTeleport(
                                    name,
                                    controller.children.first(),
                                    scene.findObserver()!!.spatial(),
                                    hmd
                                )
                                hmd.addBehaviour(name, wrapForAnalogInputIfNeeded(scene, button, behaviour))
                                hmd.addKeyBinding(name, device.role, button)
                            }
                        }
                    }
                }
            }
        }
    }
}