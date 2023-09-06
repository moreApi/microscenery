package microscenery.VRUI.behaviors

import graphics.scenery.Scene
import graphics.scenery.attribute.spatial.Spatial
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.behaviours.VRScale
import graphics.scenery.controls.behaviours.VRTwoHandDragBehavior
import graphics.scenery.controls.behaviours.VRTwoHandDragOffhand
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plusAssign
import graphics.scenery.utils.extensions.times
import microscenery.UP
import microscenery.stageSpace.StageSpaceManager
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.concurrent.CompletableFuture


class VR2HandSpatialManipulation(
    name: String,
    controller: Spatial,
    offhand: VRTwoHandDragOffhand,
    val scene: Scene,
    var scaleLocked: Boolean = false,
    var rotationLocked: Boolean = false,
    val stageSpaceManager: StageSpaceManager?
) : VRTwoHandDragBehavior(name, controller, offhand) {


    override fun dragDelta(
        currentPositionMain: Vector3f,
        currentPositionOff: Vector3f,
        lastPositionMain: Vector3f,
        lastPositionOff: Vector3f
    ) {
        val scaleDelta =
            VRScale.getScaleDelta(currentPositionMain, currentPositionOff, lastPositionMain, lastPositionOff)

        val newRein = (currentPositionMain - currentPositionOff).normalize()
        newRein.y = 0f
        val oldRein = (lastPositionMain - lastPositionOff).normalize()
        oldRein.y = 0f

        val newReinRotation = Quaternionf().lookAlong(newRein, UP)
        val oldReinRotation = Quaternionf().lookAlong(oldRein, UP)
        val diffRotation = oldReinRotation.mul(newReinRotation.invert())

        stageSpaceManager?.let {
            val target = stageSpaceManager.scaleAndRotationPivot.spatial()
            val pivot =
                stageSpaceManager.sliceManager.stacks.firstOrNull()?.volume?.spatial()?.worldPosition() ?: return@let

            if (!rotationLocked) { //this.rotation.mul(diff)
                val rot = Matrix4f().translate(pivot).rotate(diffRotation).translate(pivot.times(-1f))
                target.position += rot.getTranslation(Vector3f())
                target.rotation.mul(rot.getNormalizedRotation(Quaternionf()))
            }
            if (!scaleLocked) {
                // pivot and target are in same space
                for (i in 0..2) {
                    target.position.setComponent(i, (target.position[i] + pivot[i] * (scaleDelta - 1)))
                }
                target.scale *= scaleDelta
            }

            target.needsUpdate = true
        }
    }

    companion object {
        /**
         * Convenience method for adding scale behaviour
         */
        fun createAndSet(
            hmd: OpenVRHMD,
            button: OpenVRHMD.OpenVRButton,
            scene: Scene,
            scaleLocked: Boolean = false,
            rotationLocked: Boolean = false,
            stageSpaceManager: StageSpaceManager?,
        ): CompletableFuture<VR2HandSpatialManipulation> {
            @Suppress("UNCHECKED_CAST") return createAndSet(
                hmd, button
            ) { controller: Spatial, offhand: VRTwoHandDragOffhand ->
                VR2HandSpatialManipulation(
                    "Scaling",
                    controller,
                    offhand,
                    scene,
                    scaleLocked,
                    rotationLocked,
                    stageSpaceManager
                )
            } as CompletableFuture<VR2HandSpatialManipulation>
        }
    }
}

