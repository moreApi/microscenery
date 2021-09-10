package microscenery.behaviors

import graphics.scenery.Camera
import graphics.scenery.Node
import graphics.scenery.Scene
import graphics.scenery.attribute.spatial.Spatial
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackedDeviceType
import graphics.scenery.controls.TrackerRole
import graphics.scenery.controls.behaviours.Grabable
import graphics.scenery.controls.behaviours.VRGrab
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plusAssign
import org.joml.Vector3f

class VRGrabWithSelfMove(
    name: String,
    controllerHitbox: Node,
    targets: () -> List<Node>,
    multiTarget: Boolean,
    private val cam: Spatial?,
    onGrab: (() -> Unit)?
) : VRGrab(name, controllerHitbox, targets, multiTarget, onGrab) {

    var camDiff = Vector3f()

    override fun init(x: Int, y: Int) {
        super.init(x, y)
        cam?.let {
            camDiff = controllerSpatial.worldPosition() - cam.position
        }
    }

    override fun drag(x: Int, y: Int) {
        if (selected.isEmpty() && cam != null){
            //grabbed world
            val newCamDiff = controllerSpatial.worldPosition()-cam.position
            val diffTranslation = camDiff - newCamDiff //reversed
            cam.position += diffTranslation
            camDiff = newCamDiff
        }
        super.drag(x, y)
    }
    companion object {

        /**
         * Convenience method for adding grab behaviour
         */
        fun createAndSet(
            scene: Scene,
            hmd: OpenVRHMD,
            button: List<OpenVRHMD.OpenVRButton>,
            controllerSide: List<TrackerRole>
        ) {
            hmd.events.onDeviceConnect.add { _, device, _ ->
                if (device.type == TrackedDeviceType.Controller) {
                    device.model?.let { controller ->
                        if (controllerSide.contains(device.role)) {
                            val name = "VRDrag:${hmd.trackingSystemName}:${device.role}:$button"
                            val grabBehaviour = VRGrabWithSelfMove(
                                name,
                                controller.children.first(),
                                { scene.discover(scene, { n -> n.getAttributeOrNull(Grabable::class.java) != null }) },
                                false,
                                scene.findObserver()!!.spatial(),
                                { (hmd as? OpenVRHMD)?.vibrate(device) })

                            hmd.addBehaviour(name, grabBehaviour)
                            button.forEach {
                                hmd.addKeyBinding(name, device.role, it)
                            }
                        }
                    }
                }
            }
        }
    }
}