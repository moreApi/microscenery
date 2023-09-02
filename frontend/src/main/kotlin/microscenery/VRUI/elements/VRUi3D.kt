package microscenery.VRUI.elements

import graphics.scenery.Scene
import graphics.scenery.attribute.spatial.Spatial
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackedDeviceType
import graphics.scenery.controls.TrackerInput
import graphics.scenery.controls.TrackerRole
import graphics.scenery.controls.behaviours.WheelMenu
import microscenery.detach
import org.joml.Quaternionf
import org.joml.Vector3f
import org.scijava.ui.behaviour.DragBehaviour
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class VRUi3D(
    val controller: Spatial,
    val scene: Scene,
    val hmd: TrackerInput,
    var trackingMode: WheelMenu.TrackingMode = WheelMenu.TrackingMode.LIVE,
    var ui: Column,
) : DragBehaviour {


    init {
        ui.update.add {
            if (trackingMode == WheelMenu.TrackingMode.LIVE) {
                ui.spatial {
                    rotation = Quaternionf(hmd.getOrientation()).conjugate().normalize()
                }
                ui.spatial().position = controller.worldPosition()
            }
        }
    }


    /**
     * This function is called by the framework. Usually you don't need to call this.
     */
    override fun init(x: Int, y: Int) {
        if (ui.parent == null){
            ui.spatial {
                position = controller.worldPosition()
                if (trackingMode == WheelMenu.TrackingMode.START) {
                    rotation = Quaternionf(hmd.getOrientation()).conjugate().normalize()
                }
                scale = Vector3f(0.07f)
            }
            scene.addChild(ui)
        } else {
            ui.detach()
        }
    }

    /**
     * This function is called by the framework. Usually you don't need to call this.
     */
    override fun drag(x: Int, y: Int) {
    }

    /**
     * This function is called by the framework. Usually you don't need to call this.
     */
    override fun end(x: Int, y: Int) {
    }

    /**
     * Contains Convenience method for adding tool select behaviour
     */
    companion object {

        /**
         * Convenience method for adding tool select behaviour
         */
        fun createAndSet(
            scene: Scene,
            hmd: OpenVRHMD,
            button: List<OpenVRHMD.OpenVRButton>,
            controllerSide: List<TrackerRole>,
            trackingMode: WheelMenu.TrackingMode = WheelMenu.TrackingMode.LIVE,
            ui: Column,
        ): Future<VRUi3D> {
            val future = CompletableFuture<VRUi3D>()
            hmd.events.onDeviceConnect.add { _, device, _ ->
                if (device.type == TrackedDeviceType.Controller) {
                    device.model?.let { controller ->
                        if (controllerSide.contains(device.role)) {
                            val name = "Ui3DWindow:${hmd.trackingSystemName}:${device.role}:$button"
                            val behavior = VRUi3D(
                                controller.children.first().spatialOrNull()
                                    ?: throw IllegalArgumentException("The target controller needs a spatial."),
                                scene,
                                hmd,
                                trackingMode,
                                ui
                            )
                            hmd.addBehaviour(name, behavior)
                            button.forEach {
                                hmd.addKeyBinding(name, device.role, it)
                            }
                            future.complete(behavior)
                        }
                    }
                }
            }
            return future
        }
    }
}
