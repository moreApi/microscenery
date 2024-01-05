package microscenery.stageSpace

import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.behaviours.*
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import microscenery.MicroscenerySettings
import microscenery.nowMillis
import microscenery.signals.HardwareDimensions
import org.joml.Vector3f

class FrameGizmo(
    val stageSpaceManager: StageSpaceManager,
    hwd: HardwareDimensions,
) : Frame(hwd) {

    var mode = Mode.PASSIVE
        set(value) {
            field = value
            modeChanged(value)
        }

    var stackStartPos = Vector3f()
        private set

    init {
        // ui interaction
        beams.forEach { beam ->
            beam.addAttribute(Grabable::class.java, Grabable(target = {this}, lockRotation = true))
            beam.addAttribute(Touchable::class.java, Touchable())
            beam.addAttribute(
                Pressable::class.java, PerButtonPressable(
                    mapOf(OpenVRHMD.OpenVRButton.Trigger to SimplePressable(onRelease = {  _,_ ->stageSpaceManager.snapSlice() }))
                )
            )
        }

        var lastUpdate = 0L
        this.update += {
            spatial {

                val coerced = stageSpaceManager.hardware.hardwareDimensions().coercePosition(position,null)
                if (position != coerced) position = coerced

                when(mode){
                    Mode.PASSIVE -> {}
                    Mode.STEERING ->
                        if (position != stageSpaceManager.stagePosition
                            && lastUpdate + MicroscenerySettings.get("Stage.PositionUpdateRate", 500) < nowMillis()
                        ) {
                            stageSpaceManager.stagePosition = position
                            lastUpdate = nowMillis()
                        }
                    Mode.STACK_SELECTION -> {
                        val scanAxis = stageSpaceManager.layout.sheet.vector
                        val lockedAxis = stackStartPos * (Vector3f(1f) - scanAxis)
                        position = position * scanAxis + lockedAxis
                    }
                }
            }
        }
    }

    private fun modeChanged(mode: Mode) {
        when (mode){
            Mode.PASSIVE -> {}
            Mode.STEERING -> {}
            Mode.STACK_SELECTION -> {
                stackStartPos = this.spatial().position
            }
        }
    }

    enum class Mode{
        PASSIVE, STEERING, STACK_SELECTION
    }
}