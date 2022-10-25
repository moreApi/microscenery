package microscenery

import graphics.scenery.Box
import graphics.scenery.RichNode
import graphics.scenery.Scene
import graphics.scenery.attribute.spatial.HasSpatial
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.behaviours.*
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import graphics.scenery.volumes.TransferFunction
import microscenery.hardware.MicroscopeHardware
import microscenery.signals.HardwareDimensions
import microscenery.signals.MicroscopeStatus
import microscenery.signals.Slice
import org.joml.Vector3f
import java.util.concurrent.TimeUnit
//TODO show stage limits
/**
 * Handles in and output events concerning the microscope.
 *
 * Constructor waits until microscope is ready.
 */
class StageSpaceManager(val hardware: MicroscopeHardware, val scene: Scene, val scaleDownFactor: Float = 200f, addFocusFrame: Boolean = false) :
    Agent() {

    val stageRoot = RichNode("stage root")
    var focusFrame: HasSpatial? = null
    var currentHardwareDimensions = HardwareDimensions.EMPTY
        set(value) {
            field = value
            stageRoot.spatial().scale = value.vertexSize.times(1 / scaleDownFactor)
            focusFrame?.children?.first()?.spatialOrNull()
                ?.scale = Vector3f(value.imageSize.x * 1f, value.imageSize.y * 1f, 1f)
        }

    private val tf = TransferFunction.ramp()
    private val tfRangeMin : Float = 0.0f
    private val tfRangeMax : Float = 100.0f
    var tfOffset = 0.0f
    var tfScale = 0.0f

    val stagePosition: Vector3f
        get() = hardware.status().stagePosition


    init {
        scene.addChild(stageRoot)
        calculateOffsetAndScale()
        buildFocusFrame()

        if (addFocusFrame)
            focusFrame = buildFocusFrame().apply {
                stageRoot.addChild(this)
            }

        startAgent()
    }

    private fun calculateOffsetAndScale()
    {
        //Rangescale is either 255 or 65535
        val rangeScale = 255.0f
        val fmin = tfRangeMin / rangeScale
        val fmax = tfRangeMax / rangeScale
        tfScale = 1.0f / ( fmax - fmin )
        tfOffset = -fmin * tfScale
    }

    // build frame for focus
    private fun buildFocusFrame(): RichNode {

        val focusFrame = RichNode("focus").apply {
            this.update += {
                val min = currentHardwareDimensions.stageMin
                val max = currentHardwareDimensions.stageMax

                spatial {
                    val coerced = Vector3f()
                    position.min(max,coerced)
                    coerced.max(min)

                    if (position != coerced) position = coerced
                }
            }
        }

        // this is needed so VRGrab applies the correct scaling to the translation
        val pivot = RichNode("scalePivot").apply {
            focusFrame.addChild(this)
        }

        val beamBase = Vector3f(.1f, .1f, 1f)
        listOf(
            Vector3f(0f, 1f, 0f),
            Vector3f(0f, -1f, 0f),
            Vector3f(-1f, 0f, 0f),
            Vector3f(1f, 0f, 0f)
        ).forEach { posNorm ->
            val pos = Vector3f(0.55f) * posNorm
            val beamDir = Vector3f(1.1f, 1.1f, 0f) - posNorm.absolute(Vector3f()) * 1.1f
            val beam = Box(beamBase + beamDir)
            beam.spatial().position = pos
            pivot.addChild(beam)
        }

        pivot.children.forEach {
            it.addAttribute(Grabable::class.java, Grabable(target = focusFrame, lockRotation = true))
            it.addAttribute(Touchable::class.java, Touchable())
            it.addAttribute(
                Pressable::class.java,
                PerButtonPressable(mapOf(OpenVRHMD.OpenVRButton.Trigger to SimplePressable(onRelease = {
                    this.snapSlice(focusFrame.spatial().position)
                })))
            )
        }
        return focusFrame
    }

    override fun onLoop() {
        val signal = hardware.output.poll(200, TimeUnit.MILLISECONDS)
        when (signal) {
            is Slice -> {
                if (signal.data == null) return
                val hwd = hardware.hardwareDimensions()

                val node = SliceRenderNode(signal.data, hwd.imageSize.x, hwd.imageSize.y, 1f, hwd.numericType.bytes, tf, tfOffset, tfScale)
                node.spatial().position = signal.stagePos
                stageRoot.addChild(node)
            }
            is HardwareDimensions -> { currentHardwareDimensions = signal}
            is MicroscopeStatus -> {
                //scalePivot?.spatial()?.position = signal.stagePosition
            }
            else -> {}
        }

    }


    fun snapSlice(target: Vector3f) {
        hardware.snapSlice(target)
    }

    fun live(b: Boolean) {

    }
}

