package microscenery.stageSpace

import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.behaviours.*
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import microscenery.MicroscenerySettings
import microscenery.UI.UIModel
import microscenery.nowMillis
import org.joml.Vector3f
import kotlin.math.absoluteValue

/**
 * Tool visualizing the desired target position of the stage/focus. In VR it can also be pressed to trigger a snap command.
 */
class FrameGizmo(
    val stageSpaceManager: StageSpaceManager,
    uiModel: UIModel,
) : Frame(uiModel) {

    var mode = Mode.PASSIVE
        set(value) {
            field = value
            modeChanged(value)
        }

    var stackStartPos = Vector3f()
        private set

    init {
        // ui interaction

        var dragStartPos: Vector3f? = null
        var dragStartMode = mode

        beams.forEach { beam ->
            beam.addAttribute(Grabable::class.java, Grabable(target = {this}, lockRotation = true))
            beam.addAttribute(Touchable::class.java, Touchable())
            beam.addAttribute(
                Pressable::class.java, PerButtonPressable(
                    mapOf(OpenVRHMD.OpenVRButton.Trigger to SimplePressable(
                        // dragging while snaping results in a stack, but we dont want to penalize miss clicks when someone
                        // strangely decides to start stack acquisition via desktop gui
                        onPress = { _,_ ->
                            if (mode == Mode.STACK_SELECTION) return@SimplePressable
                            dragStartPos = this.spatial().position
                            dragStartMode = mode
                                  },
                        onHold = { _, _ ->
                            if (((dragStartPos?.z ?: 0f)- spatial().position.z).absoluteValue
                                > MicroscenerySettings.get("Stage.precisionZ", 1f) * 5) {
                                // activate only on a sufficently large drag
                                // todo respect stageManager.layout
                                mode = Mode.STACK_SELECTION
                            }
                        },
                        onRelease = {  _,_ ->
                            if (mode == Mode.STACK_SELECTION && dragStartPos != null){
                                stageSpaceManager.stack(dragStartPos!!, spatial().position)
                            } else {
                                stageSpaceManager.snapSlice()
                            }
                            dragStartPos = null
                            mode = dragStartMode
                        }))
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