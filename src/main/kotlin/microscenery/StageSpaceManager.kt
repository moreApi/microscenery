package microscenery

import graphics.scenery.*
import graphics.scenery.attribute.spatial.HasSpatial
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.behaviours.*
import graphics.scenery.utils.LazyLogger
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import graphics.scenery.volumes.HasTransferFunction
import graphics.scenery.volumes.TransferFunction
import microscenery.hardware.MicroscopeHardware
import microscenery.signals.HardwareDimensions
import microscenery.signals.MicroscopeStatus
import microscenery.signals.Slice
import org.joml.Vector3f
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

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
) : Agent(), HasTransferFunction {

    private val logger by LazyLogger()

    val stageRoot = RichNode("stage root")
    var focusFrame: FocusFrame? = null

    var sortedSlices = ArrayList<SliceRenderNode>()

    private var tfOffset = 0.0f
    private var tfScale = 0.0f

    var stagePosition: Vector3f
        get() = hardware.status().stagePosition
        set(value) {
            hardware.stagePosition = value
            updateSlices()
        }

    override var maxDisplayRange: Float = 1000.0f
        set(value)
        {
            field = value
            calculateOffsetAndScale()
            updateSlices()
        }
    override var minDisplayRange: Float = 0.0f
        set(value)
        {
            field = value
            calculateOffsetAndScale()
            updateSlices()
        }
    override var transferFunction : TransferFunction = TransferFunction.ramp(0.0f, 1.0f, 0.5f)
        set(value) {
            field = value
            updateSlices()
        }

    init {
        scene.addChild(stageRoot)
        calculateOffsetAndScale()

        if (addFocusFrame)
            focusFrame = FocusFrame(this, hardware.hardwareDimensions()).apply {
                stageRoot.addChild(this)
            }

        startAgent()
    }

    /**
     * Iterates over all slices and updates their transferFunction, offset and scale values according to the currently set values of this manager
     */
    private fun updateSlices()
    {
        sortedSlices.forEach {
            it.transferFunction = transferFunction
            it.material().metallic = tfOffset
            it.material().roughness = tfScale
        }
    }

    /**
     * This normally happens inside the converter of a volume. Converts the minDisplayRange and maxDisplayRange values into an offset and scale used inside the shader
     */
    private fun calculateOffsetAndScale() {
        //Rangescale is either 255 or 65535
        val rangeScale = 255.0f
        val fmin = minDisplayRange / rangeScale
        val fmax = maxDisplayRange / rangeScale
        tfScale = 1.0f / (fmax - fmin)
        tfOffset = -fmin * tfScale
    }

    /**
     * Inserts a slice into the local sliceContainer and sorts it using its z coordinate -> TODO: Make this use the camera and sort by view-vector
     */
    private fun insertSlice(slice : SliceRenderNode) : Int
    {
        sortedSlices.add(slice)
        sortedSlices.sortBy { it.spatial().position.z() }
        val index = sortedSlices.indexOf(slice)
        return index
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
                    transferFunction
                )
                node.material().metallic = tfOffset
                node.material().roughness = tfScale
                node.spatial().position = signal.stagePos
                stageRoot.addChild(node)

                //now follows insurance, that slides get ordered correctly
                val index = insertSlice(node)
                stageRoot.children.remove(node)
                stageRoot.children.add(index, node)
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
            val focusFrame = this

            this.update += {

                spatial {
                    val coerced = Vector3f()
                    position.min(stageMax, coerced)
                    coerced.max(stageMin)

                    if (position != coerced) position = coerced

                    if (position != stageSpaceManager.stagePosition) stageSpaceManager.stagePosition = position
                }
            }


            // this is needed so VRGrab applies the correct scaling to the translation
            pivot = RichNode("scalePivot").apply {
                focusFrame.addChild(this)
            }

            val beamBase = Vector3f(.1f, .1f, 1f)
            // beams
            listOf(
                Vector3f(0f, 1f, 0f),
                Vector3f(0f, -1f, 0f),
                Vector3f(-1f, 0f, 0f),
                Vector3f(1f, 0f, 0f)
            ).map { posNorm ->
                // position
                val pos = Vector3f(0.55f) * posNorm
                val beamDir = Vector3f(1.1f, 1.1f, 0f) - posNorm.absolute(Vector3f()) * 1.1f
                val beam = Box(beamBase + beamDir)
                beam.spatial().position = pos
                pivot.addChild(beam)

                // ui interaction
                beam.addAttribute(Grabable::class.java, Grabable(target = focusFrame, lockRotation = true))
                beam.addAttribute(Touchable::class.java, Touchable())
                beam.addAttribute(
                    Pressable::class.java,
                    PerButtonPressable(mapOf(OpenVRHMD.OpenVRButton.Trigger to SimplePressable(onRelease = {
                        stageSpaceManager.snapSlice()
                    })))
                )
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

