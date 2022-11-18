package microscenery.stageSpace

import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.behaviours.*
import microscenery.MicroscenerySettings
import microscenery.nowMillis
import microscenery.signals.HardwareDimensions
import org.joml.Vector3f

class FocusFrame(
    val stageSpaceManager: StageSpaceManager,
    hwd: HardwareDimensions,
    var stageSteeringActive: Boolean = false
) : Frame(hwd) {

    init {
        // ui interaction
        beams.forEach { beam ->
            beam.addAttribute(Grabable::class.java, Grabable(target = this, lockRotation = true))
            beam.addAttribute(Touchable::class.java, Touchable())
            beam.addAttribute(
                Pressable::class.java, PerButtonPressable(
                    mapOf(OpenVRHMD.OpenVRButton.Trigger to SimplePressable(onRelease = { stageSpaceManager.snapSlice() }))
                )
            )
        }

        var lastUpdate = 0L
        this.update += {
            spatial {
                val coerced = Vector3f()
                position.min(stageMax, coerced)
                coerced.max(stageMin)

                if (position != coerced) position = coerced

                if (stageSteeringActive
                    && position != stageSpaceManager.stagePosition
                    && lastUpdate + MicroscenerySettings.get("Stage.PositionUpdateRate", 500) < nowMillis()
                ) {
                    stageSpaceManager.stagePosition = position
                    lastUpdate = nowMillis()
                }

            }
        }
    }
}