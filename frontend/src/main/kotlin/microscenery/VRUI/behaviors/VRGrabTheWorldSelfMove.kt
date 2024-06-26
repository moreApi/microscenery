package microscenery.VRUI.behaviors

import graphics.scenery.Node
import graphics.scenery.Scene
import graphics.scenery.attribute.spatial.Spatial
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackedDeviceType
import graphics.scenery.controls.TrackerRole
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plusAssign
import microscenery.wrapForAnalogInputIfNeeded
import org.joml.Vector3f
import org.scijava.ui.behaviour.DragBehaviour

class VRGrabTheWorldSelfMove(
    @Suppress("UNUSED_PARAMETER") name: String,
    controllerHitbox: Node,
    private val cam: Spatial,
) : DragBehaviour {

    var camDiff = Vector3f()

    protected val controllerSpatial: Spatial = controllerHitbox.spatialOrNull()
        ?: throw IllegalArgumentException("controller hitbox needs a spatial attribute")


    override fun init(x: Int, y: Int) {
        camDiff = controllerSpatial.worldPosition() - cam.position
    }

    override fun drag(x: Int, y: Int) {
        //grabbed world
        val newCamDiff = controllerSpatial.worldPosition() - cam.position
        val diffTranslation = camDiff - newCamDiff //reversed
        cam.position += diffTranslation
        camDiff = newCamDiff
    }

    override fun end(x: Int, y: Int) {
    }

    companion object {

        /**
         * Convenience method for adding grab behaviour
         */
        fun createAndSet(
            scene: Scene, hmd: OpenVRHMD, buttons: List<OpenVRHMD.OpenVRButton>, controllerSide: List<TrackerRole>
        ) {
            hmd.events.onDeviceConnect.add { _, device, _ ->
                if (device.type == TrackedDeviceType.Controller) {
                    device.model?.let { controller ->
                        if (controllerSide.contains(device.role)) {
                            buttons.forEach { button ->
                                val name = "VRDrag:${hmd.trackingSystemName}:${device.role}:$button"
                                val grabBehaviour = VRGrabTheWorldSelfMove(
                                    name, controller.children.first(), scene.findObserver()!!.spatial()
                                )

                                hmd.addBehaviour(name, wrapForAnalogInputIfNeeded(scene, button, grabBehaviour))
                                hmd.addKeyBinding(name, device.role, button)
                            }
                        }
                    }
                }
            }
        }
    }
}