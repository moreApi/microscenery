package microscenery.stageSpace

import graphics.scenery.RichNode
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import microscenery.*
import microscenery.UI.UIModel
import org.joml.Vector3f

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
        }

        stageSpaceManager.stageRoot.addChild(focusTarget)
        focusTarget.spatial().position = stageSpaceManager.hardware.stagePosition.copy()

        focusTargetIndicator = Frame(uiModel, Vector3f(0.2f,0.2f,1f)) { focusTarget.spatial().position }.also {
            focusTarget.addChild(it)
            it.spatialOrNull()?.rotation = stageSpaceManager.layout.sheetRotation()
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

    private fun initVRInteraction(){
        /**
         * var dragStartPos: Vector3f? = null
         *         var dragStartMode = mode
         *
         *         beams.forEach { beam ->
         *             beam.addAttribute(Grabable::class.java, Grabable(target = {this}, lockRotation = true))
         *             beam.addAttribute(Touchable::class.java, Touchable())
         *             beam.addAttribute(
         *                 Pressable::class.java, PerButtonPressable(
         *                     mapOf(OpenVRHMD.OpenVRButton.Trigger to SimplePressable(
         *                         // dragging while snaping results in a stack, but we dont want to penalize miss clicks when someone
         *                         // strangely decides to start stack acquisition via desktop gui
         *                         onPress = { _,_ ->
         *                             if (mode == Mode.STACK_SELECTION) return@SimplePressable
         *                             dragStartPos = this.spatial().position
         *                             dragStartMode = mode
         *                                   },
         *                         onHold = { _, _ ->
         *                             if (((dragStartPos?.z ?: 0f)- spatial().position.z).absoluteValue
         *                                 > MicroscenerySettings.get("Stage.precisionZ", 1f) * 5) {
         *                                 // activate only on a sufficently large drag
         *                                 // todo respect stageManager.layout
         *                                 mode = Mode.STACK_SELECTION
         *                             }
         *                         },
         *                         onRelease = {  _,_ ->
         *                             if (mode == Mode.STACK_SELECTION && dragStartPos != null){
         *                                 stageSpaceManager.stack(dragStartPos!!, spatial().position)
         *                             } else {
         *                                 stageSpaceManager.snapSlice()
         *                             }
         *                             dragStartPos = null
         *                             mode = dragStartMode
         *                         }))
         *                 )
         *             )
         *         }
         */
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
                stackStartPos = focusTarget.spatial().position
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