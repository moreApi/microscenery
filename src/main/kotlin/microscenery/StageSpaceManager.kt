package microscenery

import graphics.scenery.*
import graphics.scenery.utils.LazyLogger
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.times
import graphics.scenery.volumes.HasTransferFunction
import graphics.scenery.utils.extensions.toFloatArray
import graphics.scenery.volumes.BufferedVolume
import graphics.scenery.volumes.TransferFunction
import graphics.scenery.volumes.Volume
import microscenery.VRUI.FocusFrame
import microscenery.hardware.MicroscopeHardware
import microscenery.signals.*
import net.imglib2.type.numeric.integer.UnsignedByteType
import net.imglib2.type.numeric.integer.UnsignedShortType
import org.joml.Vector3f
import java.util.concurrent.TimeUnit
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import kotlin.concurrent.withLock

//TODO show stage limits
/**
 * Handles in and output events concerning the microscope.
 *
 * Constructor waits until microscope is ready.
 */
class StageSpaceManager(
    val hardware: MicroscopeHardware,
    val scene: Scene,
    val hub: Hub,
    addFocusFrame: Boolean = true,
    val scaleDownFactor: Float = 200f
) : Agent(), HasTransferFunction {
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))


    val stageRoot = RichNode("stage root")
    var focusFrame: FocusFrame? = null

    private var sortedSlices = ArrayList<SliceRenderNode>()
    private var stacks = emptyList<StackContainer>()

    private var tfOffset = 0.0f
    private var tfScale = 0.0f

    var stagePosition: Vector3f
        get() = hardware.status().stagePosition
        set(value) {
            hardware.stagePosition = value
        }

    override var minDisplayRange: Float = 0.0f
        set(value)
        {
            field = value
            calculateOffsetAndScale()
            updateSlices()
        }
    override var maxDisplayRange: Float = 1000.0f
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
                spatial().position = hardware.stagePosition
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
            it.transferFunctionOffset = tfOffset
            it.transferFunctionScale = tfScale
        }
        stacks.forEach {
            it.volume.transferFunction = transferFunction
            it.volume.minDisplayRange = minDisplayRange
            it.volume.maxDisplayRange = maxDisplayRange
        }
    }

    /**
     * This normally happens inside the converter of a volume. Converts the minDisplayRange and maxDisplayRange values into an offset and scale used inside the shader
     */
    private fun calculateOffsetAndScale() {
        // Rangescale is either 255 or 65535
        val rangeScale = when(hardware.hardwareDimensions().numericType){
            NumericType.INT8 -> 255
            NumericType.INT16 -> 65535
        }
        val fmin = minDisplayRange / rangeScale
        val fmax = maxDisplayRange / rangeScale
        tfScale = 1.0f / (fmax - fmin)
        tfOffset = -fmin * tfScale
    }

    /**
     * Inserts a slice into the local sliceContainer and sorts it using its z coordinate -> TODO: Make this use the camera and sort by view-vector
     */
    private fun insertSlice(slice: SliceRenderNode): Int {
        sortedSlices.add(slice)
        sortedSlices.sortBy { it.spatial().position.z() }
        return sortedSlices.indexOf(slice)
    }

    override fun onLoop() {
        val signal = hardware.output.poll(200, TimeUnit.MILLISECONDS)
        when (signal) {
            is Slice -> {
                if (signal.data == null) return

                if (signal.stackId != null && stacks.any { it.meta.Id == signal.stackId }) {
                    // slice belongs to a stack
                    handleStackSlice(signal)
                    return
                }
                // slice does not belong to a stack and should be visualised on its own
                handleSingleSlice(signal)
            }
            is HardwareDimensions -> {
                stageRoot.spatial().scale = signal.vertexSize.times(1 / scaleDownFactor)
                focusFrame?.applyHardwareDimensions(signal)
            }
            is MicroscopeStatus -> {
                focusFrame?.spatial()?.position = signal.stagePosition
            }
            is Stack -> {
                val stack = signal
                val buffer =
                    MemoryUtil.memAlloc(stack.size.x * stack.size.y * stack.size.z
                            * hardware.hardwareDimensions().numericType.bytes)
                val volume = when (hardware.hardwareDimensions().numericType) {
                    NumericType.INT8 -> Volume.fromBuffer(
                        listOf(BufferedVolume.Timepoint("0", buffer)),
                        stack.size.x, stack.size.y, stack.size.z,
                        UnsignedByteType(),
                        hub, hardware.hardwareDimensions().vertexSize.toFloatArray()
                    )
                    NumericType.INT16 -> Volume.fromBuffer(
                        listOf(BufferedVolume.Timepoint("0", buffer)),
                        stack.size.x, stack.size.y, stack.size.z,
                        UnsignedShortType(),
                        hub, hardware.hardwareDimensions().vertexSize.toFloatArray()
                    )
                }
                volume.goToLastTimepoint()
                volume.transferFunction = transferFunction
                volume.name = "Stack ${signal.Id}"
                //volume.spatial().scale = Vector3f(10f)
                volume.origin = Origin.FrontBottomLeft
                volume.spatial().position = stack.stageMin
                volume.pixelToWorldRatio = 1f // conversion is done by stage root
                volume.setTransferFunctionRange(17.0f, 3000.0f)

                stageRoot.addChild(volume)

                BoundingGrid().apply {
                    this.node = volume
                    //volume.metadata["BoundingGrid"] = this
                }
                stacks = stacks + StackContainer(stack, volume, buffer)
            }
        }
    }

    private fun handleStackSlice(slice: Slice) {
        if (slice.data == null) return

        val stack = stacks.find { it.meta.Id == slice.stackId }
        if (stack == null) {
            logger.error("Did not find stack id: ${slice.stackId}")
            return
        }

        val sliceIndex = ((slice.stagePos - stack.meta.stageMin).z / stack.meta.voxelSize.z).toInt()

        stack.buffer.position(slice.size * sliceIndex)
        stack.buffer.put(slice.data)
        stack.buffer.rewind()
        stack.volume.goToNewTimepoint(stack.buffer)
    }

    private fun handleSingleSlice(signal: Slice) {
        if (signal.data == null) return
        val hwd = hardware.hardwareDimensions()

        val node = SliceRenderNode(
            signal.data,
            hwd.imageSize.x,
            hwd.imageSize.y,
            1f,
            hwd.numericType.bytes,
            transferFunction,
            tfOffset,
            tfScale
        )
        node.spatial().position = signal.stagePos

        val minDistance = hardware.hardwareDimensions().vertexSize.length() * 1
        stageRoot.children.filter {
            it is SliceRenderNode && it.spatialOrNull()?.position?.equals(
                signal.stagePos,
                minDistance
            ) ?: false
        }
            .toList() // get out of children.iterator or something, might be bad to do manipulation within an iterator
            .forEach {
                stageRoot.removeChild(it)
                sortedSlices.remove(it)
            }
        stageRoot.addChild(node)

        val index = insertSlice(node)
        //now follows insurance, that slides get ordered correctly
        stageRoot.children.remove(node)
        stageRoot.children.add(index, node)
    }

    fun stack(from: Vector3f, to: Vector3f) {
        hardware.acquireStack(
            ClientSignal.AcquireStack(
                from, to, hardware.hardwareDimensions().vertexSize.z
            )
        )
    }

    fun snapSlice() {
        hardware.snapSlice()
    }

    fun live(b: Boolean) {
        hardware.live = b
    }

    private class StackContainer(val meta: Stack, val volume: BufferedVolume, val buffer: ByteBuffer)

    companion object {
        private fun BufferedVolume.goToNewTimepoint(buffer: ByteBuffer) {
            val volume = this
            volume.lock.withLock {
                val count = volume.timepoints?.lastOrNull()?.name?.toIntOrNull() ?: 0
                volume.addTimepoint("${count + 1}", buffer)
                logger.info("going to Timepoint ${volume.goToLastTimepoint()}")
                volume.purgeFirst(3, 3)
            }
        }
    }
}

