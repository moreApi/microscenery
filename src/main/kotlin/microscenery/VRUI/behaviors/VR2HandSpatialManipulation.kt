package microscenery.VRUI.behaviors

import graphics.scenery.Scene
import graphics.scenery.attribute.spatial.Spatial
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.behaviours.VRScale
import graphics.scenery.controls.behaviours.VRTwoHandDragBehavior
import graphics.scenery.controls.behaviours.VRTwoHandDragOffhand
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.times
import graphics.scenery.volumes.Volume
import microscenery.UP
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.concurrent.CompletableFuture


class VR2HandSpatialManipulation  (name: String,
                                   controller: Spatial,
                                   offhand: VRTwoHandDragOffhand,
                                   val scene:Scene,
                                   var scaleLocked: Boolean = false
) : VRTwoHandDragBehavior(name, controller, offhand)  {

    override fun dragDelta(
                       currentPositionMain: Vector3f,
                       currentPositionOff: Vector3f,
                       lastPositionMain: Vector3f,
                       lastPositionOff: Vector3f
    ){
        val volume = scene.findByClassname(Volume::class.java.simpleName).firstOrNull()?: return

        val scaleDelta = VRScale.getScaleDelta(currentPositionMain, currentPositionOff, lastPositionMain, lastPositionOff)

        val newRein = (currentPositionMain - currentPositionOff).normalize()
        newRein.y = 0f
        val oldRein = (lastPositionMain - lastPositionOff).normalize()
        oldRein.y = 0f

        val newReinRotation = Quaternionf().lookAlong(newRein, UP)
        val oldReinRotation = Quaternionf().lookAlong(oldRein, UP)
        val diff = oldReinRotation.mul(newReinRotation.invert())

        volume.ifSpatial {
            this.rotation.mul(diff)
            if (!scaleLocked)
                this.scale *= Vector3f(scaleDelta)
            this.needsUpdate = true
        }
    }

    companion object {
        /**
         * Convenience method for adding scale behaviour
         */
        fun createAndSet(
            hmd: OpenVRHMD, button: OpenVRHMD.OpenVRButton, scene: Scene
        ): CompletableFuture<VR2HandSpatialManipulation> {
            @Suppress("UNCHECKED_CAST") return createAndSet(
                hmd,
                button
            ) { controller: Spatial, offhand: VRTwoHandDragOffhand ->
                VR2HandSpatialManipulation("Scaling", controller, offhand, scene)
            } as CompletableFuture<VR2HandSpatialManipulation>
        }
    }
}

