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
import microscenery.MicroscenerySettings
import microscenery.Settings
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
    val stageSpaceManager: StageSpaceManager?,
    val alternativeTarget: Spatial? = null,
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


        val target = stageSpaceManager?.scaleAndRotationPivot?.spatial() ?: alternativeTarget
        val pivot = stageSpaceManager?.stageRoot?.spatial()?.worldPosition(stageSpaceManager.stageAreaCenter) ?: Vector3f()

        target?.let {
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
            rotationLocked: Boolean = MicroscenerySettings.get(Settings.VRUI.LockRotationDefault, false),
            stageSpaceManager: StageSpaceManager?,
            alternativeTarget: Spatial? = null,
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
                    stageSpaceManager,
                    alternativeTarget
                )
            } as CompletableFuture<VR2HandSpatialManipulation>
        }
    }
}

