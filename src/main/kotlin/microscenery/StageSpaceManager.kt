package microscenery

import graphics.scenery.Box
import graphics.scenery.RichNode
import graphics.scenery.Scene
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.behaviours.*
import graphics.scenery.primitives.TextBoard
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import graphics.scenery.volumes.TransferFunction
import microscenery.hardware.MicroscopeHardware
import microscenery.signals.HardwareDimensions
import microscenery.signals.MicroscopeStatus
import microscenery.signals.Slice
import org.joml.Vector3f
import org.joml.Vector4f
import java.util.concurrent.TimeUnit

//TODO show stage limits
/**
 * Handles in and output events concerning the microscope.
 *
 * Constructor waits until microscope is ready.
 */
class StageSpaceManager(
    val hardware: MicroscopeHardware,
    val scene: Scene,
    val scaleDownFactor: Float = 200f,
    addFocusFrame: Boolean = false
) : Agent() {

    val stageRoot = RichNode("stage root")
    var focusFrame: FocusFrame? = null

    private val tf = TransferFunction.ramp()
    private val tfRangeMin: Float = 0.0f
    private val tfRangeMax: Float = 100.0f
    var tfOffset = 0.0f
    var tfScale = 0.0f

    var stagePosition: Vector3f
        get() = hardware.status().stagePosition
        set(value) {
            hardware.stagePosition = value
        }


    init {
        scene.addChild(stageRoot)
        calculateOffsetAndScale()

        if (addFocusFrame)
            focusFrame = FocusFrame(this, hardware.hardwareDimensions()).apply {
                spatial().position = hardware.stagePosition
                stageRoot.addChild(this)
            }

        startAgent()
    }

    private fun calculateOffsetAndScale() {
        //Rangescale is either 255 or 65535
        val rangeScale = 255.0f
        val fmin = tfRangeMin / rangeScale
        val fmax = tfRangeMax / rangeScale
        tfScale = 1.0f / (fmax - fmin)
        tfOffset = -fmin * tfScale
    }


    override fun onLoop() {
        val signal = hardware.output.poll(200, TimeUnit.MILLISECONDS)
        when (signal) {
            is Slice -> {
                if (signal.data == null) return
                val hwd = hardware.hardwareDimensions()

                val node = SliceRenderNode(
                    signal.data,
                    hwd.imageSize.x,
                    hwd.imageSize.y,
                    1f,
                    hwd.numericType.bytes,
                    tf,
                    tfOffset,
                    tfScale
                )
                node.spatial().position = signal.stagePos
                stageRoot.addChild(node)
            }
            is HardwareDimensions -> {
                stageRoot.spatial().scale = signal.vertexSize.times(1 / scaleDownFactor)
                focusFrame?.applyHardwareDimensions(signal)

            }
            is MicroscopeStatus -> {
                focusFrame?.spatial()?.position = signal.stagePosition
            }
            else -> {}
        }
    }

    fun snapSlice() {
        hardware.snapSlice()
    }

    class FocusFrame(val stageSpaceManager: StageSpaceManager, hwd: HardwareDimensions) : RichNode("focus") {

        private var stageMin: Vector3f = Vector3f()
        private var stageMax: Vector3f = Vector3f(1f)

        private val pivot: RichNode

        init {
            // this is needed so VRGrab applies the correct scaling to the translation
            pivot = RichNode("scalePivot")
            this.addChild(pivot)

            val beamBase = Vector3f(.1f, .1f, 1f)
            val distanceFromCenter = Vector3f(0.55f)
            // beams
            listOf(
                Vector3f(0f, 1f, 0f),
                Vector3f(0f, -1f, 0f),
                Vector3f(-1f, 0f, 0f),
                Vector3f(1f, 0f, 0f)
            ).map { posNorm ->
                // position
                val pos = distanceFromCenter * posNorm
                val beamDir = Vector3f(1.1f, 1.1f, 0f) - posNorm.absolute(Vector3f()) * 1.1f
                val beam = Box(beamBase + beamDir)
                beam.spatial().position = pos
                pivot.addChild(beam)

                // ui interaction
                beam.addAttribute(Grabable::class.java, Grabable(target = this, lockRotation = true))
                beam.addAttribute(Touchable::class.java, Touchable())
                beam.addAttribute(
                    Pressable::class.java,
                    PerButtonPressable(mapOf(OpenVRHMD.OpenVRButton.Trigger to SimplePressable(onRelease = {
                        stageSpaceManager.snapSlice()
                    })))
                )
            }

            val positionLabel = TextBoard()
            positionLabel.text = "0,0,0"
            positionLabel.name = "FramePositionLabel"
            positionLabel.transparent = 0
            positionLabel.fontColor = Vector4f(0.0f, 0.0f, 0.0f, 1.0f)
            positionLabel.backgroundColor = Vector4f(100f, 100f, 100f, 1.0f)
            positionLabel.spatial {
                position = Vector3f(-distanceFromCenter.x, distanceFromCenter.y+beamBase.y, 0f)
                scale = Vector3f(0.15f, 0.15f, 0.15f)
            }
            pivot.addChild(positionLabel)

            this.update += {

                spatial {
                    val coerced = Vector3f()
                    position.min(stageMax, coerced)
                    coerced.max(stageMin)

                    if (position != coerced) position = coerced

                    if (position != stageSpaceManager.stagePosition) stageSpaceManager.stagePosition = position

                    positionLabel.text = position.toReadableString()
                }
            }
            applyHardwareDimensions(hwd)
        }

        fun applyHardwareDimensions(hwd: HardwareDimensions) {
            pivot.spatialOrNull()?.scale = Vector3f(hwd.imageSize.x.toFloat(), hwd.imageSize.y.toFloat(), 1f)
            stageMin = hwd.stageMin
            stageMax = hwd.stageMax
        }
    }
}

