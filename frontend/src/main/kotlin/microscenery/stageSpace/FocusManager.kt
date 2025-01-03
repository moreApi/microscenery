package microscenery.stageSpace

import graphics.scenery.RichNode
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackerRole
import graphics.scenery.controls.behaviours.*
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import microscenery.*
import microscenery.UI.UIModel
import microscenery.VRUI.VRHandTool
import org.joml.Vector3f
import kotlin.math.absoluteValue

class FocusManager(val stageSpaceManager: StageSpaceManager, val uiModel: UIModel) {
    val focus: Frame
    val focusTargetIndicator: Frame
    var focusTarget: RichNode = RichNode("Focus target")

    var mode = Mode.PASSIVE
        set(value) {
            field = value
            modeChanged(value)
        }
    enum class Mode{
        PASSIVE, STEERING, STACK_SELECTION
    }

    var stackStartPos = Vector3f()
        private set
    private var stackStartIndicator: Frame? = null

    init {

        focus = Frame(uiModel, Vector3f(1f)).apply {
            spatial().position = stageSpaceManager.hardware.stagePosition.copy()
            stageSpaceManager.stageRoot.addChild(this)
            visible = !MicroscenerySettings.get(Settings.StageSpace.HideFocusFrame,false)
            children.first()?.spatialOrNull()?.rotation = stageSpaceManager.layout.sheetRotation()
            // POSTSTUDY reactivate initVRInteraction(this,true)
        }

        stageSpaceManager.stageRoot.addChild(focusTarget)
        focusTarget.spatial().position = stageSpaceManager.hardware.stagePosition.copy()

        focusTargetIndicator = Frame(uiModel, Vector3f(0.2f,0.2f,1f)) { focusTarget.spatial().position }.also {
            focusTarget.addChild(it)
            it.spatialOrNull()?.rotation = stageSpaceManager.layout.sheetRotation()
            initVRInteraction(it,false)
        }

        focus.update += {
            focusTargetIndicator.visible = !MicroscenerySettings.get(Settings.StageSpace.HideFocusTargetFrame, false)
                    && focus.spatial().position != focusTarget.spatial().position
        }

        var lastUpdate = 0L

        focusTarget.update += {
            focusTarget.spatial {

                val coerced = stageSpaceManager.hardware.hardwareDimensions().coercePosition(position,null)
                if (position != coerced) position = coerced

                when(mode){
                    Mode.PASSIVE -> {}
                    Mode.STEERING ->
                        if (position != stageSpaceManager.stagePosition
                            && lastUpdate + MicroscenerySettings.get("Stage.PositionUpdateRate", 200) < nowMillis()
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

    class FocusMover(val stageSpaceManager: StageSpaceManager): RichNode("Focus Mover"), VRHandTool {
        private val focusManager = stageSpaceManager.focusManager

        private var dragStartPos: Vector3f? = null

        init {
            focusManager.mode = Mode.STEERING
            addAttribute(
                Pressable::class.java, PerButtonPressable(
                    mapOf(
                        OpenVRHMD.OpenVRButton.Trigger to SimplePressable(
                        // dragging while snaping results in a stack, but we dont want to penalize miss clicks when someone
                        // strangely decides to start stack acquisition via desktop gui
                        onPress = { _,_ ->
                            if (focusManager.mode == Mode.STACK_SELECTION) return@SimplePressable
                            dragStartPos = focusManager.focusTarget.spatial().position.copy()
                        },
                        onHold = { _, _ ->
                            val startTmp = dragStartPos?: return@SimplePressable
                            if ((startTmp.z - focusManager.focusTarget.spatial().position.z).absoluteValue
                                > MicroscenerySettings.get("Stage.precisionZ", 1f) * 5
                                && focusManager.mode != Mode.STACK_SELECTION) {
                                // activate only on a sufficently large drag
                                // todo respect stageManager.layout
                                focusManager.mode = Mode.STACK_SELECTION
                                focusManager.stackStartPos = startTmp
                                focusManager.stackStartIndicator?.spatial()?.position = startTmp

                            }
                        },
                        onRelease = {  _,_ ->
                            if (focusManager.mode == Mode.STACK_SELECTION && dragStartPos != null){
                                focusManager.mode = Mode.PASSIVE
                                stageSpaceManager.stack(dragStartPos!!, focusManager.focusTarget.spatial().position)
                            } else {
                                stageSpaceManager.snapSlice()
                            }
                            dragStartPos = null
                        }))
                )
            )
        }
    }

    private fun initVRInteraction(frame: Frame, resetTargetPos: Boolean){
        val beams = frame.beams

        beams.forEach { beam ->
            beam.addAttribute(Touchable::class.java, Touchable())
            if (resetTargetPos){
                beam.addAttribute(Pressable::class.java, SimplePressable(
                    onPress = {_,_ -> focusTarget.spatial().position = stageSpaceManager.stagePosition}
                ))
            } else {
                beam.addAttribute(Grabable::class.java, Grabable(lockRotation = true, target = { focusTarget },
                    onGrab = { uiModel.putInHand(TrackerRole.RightHand, FocusMover(stageSpaceManager)) },
                    onRelease = { uiModel.putInHand(TrackerRole.RightHand, null) }
                ))
            }
        }
    }

    private fun modeChanged(mode: Mode) {
        when (mode){
            Mode.PASSIVE -> {
                stackStartIndicator?.detach()
            }
            Mode.STEERING -> {
                stackStartIndicator?.detach()
            }
            Mode.STACK_SELECTION -> {
                stackStartPos = focusTarget.spatial().position.copy()
                stackStartIndicator?.detach()
                stackStartIndicator = Frame(uiModel,Vector3f(0.1f,0.8f,0.8f)).apply {
                    spatial().position = stackStartPos
                    stageSpaceManager.stageRoot.addChild(this)
                }
            }
        }
    }

    fun newStagePosition(pos: Vector3f){
        focus.spatial().position = pos
    }
}