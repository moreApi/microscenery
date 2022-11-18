package microscenery

import graphics.scenery.*
import graphics.scenery.attribute.material.Material
import graphics.scenery.controls.InputHandler
import graphics.scenery.utils.LazyLogger
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import graphics.scenery.utils.extensions.toFloatArray
import graphics.scenery.volumes.BufferedVolume
import graphics.scenery.volumes.HasTransferFunction
import graphics.scenery.volumes.TransferFunction
import graphics.scenery.volumes.Volume
import microscenery.UI.FocusFrame
import microscenery.UI.MovementCommand
import microscenery.hardware.MicroscopeHardware
import microscenery.signals.*
import net.imglib2.type.numeric.integer.UnsignedByteType
import net.imglib2.type.numeric.integer.UnsignedShortType
import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil
import org.scijava.ui.behaviour.ClickBehaviour
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import kotlin.concurrent.withLock

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
    val scaleDownFactor: Float = 200f,
    val layout: MicroscopeLayout = MicroscopeLayout.Default()
) : Agent(), HasTransferFunction {
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))


    val stageRoot = RichNode("stage root")
    var focusFrame: FocusFrame? = null
    private val stageAreaBorders: Box
    var stageAreaCenter = Vector3f()
        private set

    private var sortedSlices = ArrayList<SliceRenderNode>()
    private var stacks = emptyList<StackContainer>()

    private var transferFunctionOffset = 0.0f
    private var transferFunctionScale = 0.0f

    var stagePosition: Vector3f
        get() = hardware.status().stagePosition
        set(value) {
            hardware.stagePosition = value
        }

    override var minDisplayRange: Float = 0.0f
        set(value) {
            field = value
            calculateOffsetAndScale()
            updateSlices()
        }
    override var maxDisplayRange: Float = 1000.0f
        set(value) {
            field = value
            calculateOffsetAndScale()
            updateSlices()
        }
    override var transferFunction: TransferFunction = TransferFunction.ramp(0.0f, 1.0f, 0.5f)
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

        stageAreaBorders = Box(Vector3f(1f), insideNormals = true)
        stageAreaBorders.name = "stageAreaBorders"
        stageAreaBorders.material {
            ambient = Vector3f(0.6f, 0.6f, 0.6f)
            diffuse = Vector3f(0.4f, 0.4f, 0.4f)
            specular = Vector3f(0.0f, 0.0f, 0.0f)
            cullingMode = Material.CullingMode.Front
        }
        stageRoot.addChild(stageAreaBorders)
        BoundingGrid().node = stageAreaBorders

        //now it's Default(Axis.Z)
        when (layout) {
            is MicroscopeLayout.Default -> {
                if (layout.sheet != MicroscopeLayout.Axis.Z) {
                    // Z is default
                    focusFrame?.children?.first()?.spatialOrNull()?.rotation = layout.sheetRotation()
                }
            }
            is MicroscopeLayout.Scape -> {
                // turn focus frame && slices to correct axis and degree
                focusFrame?.children?.first()?.spatialOrNull()?.rotation = layout.sheetRotation()
                //todo degree rotation
            }
        }

        startAgent()
    }

    /**
     * Iterates over all slices and updates their transferFunction, offset and scale values according to the currently set values of this manager
     */
    private fun updateSlices() {
        sortedSlices.forEach {
            it.transferFunction = transferFunction
            it.transferFunctionOffset = transferFunctionOffset
            it.transferFunctionScale = transferFunctionScale
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
        val rangeScale = when (hardware.hardwareDimensions().numericType) {
            NumericType.INT8 -> 255
            NumericType.INT16 -> 65535
        }
        val fmin = minDisplayRange / rangeScale
        val fmax = maxDisplayRange / rangeScale
        transferFunctionScale = 1.0f / (fmax - fmin)
        transferFunctionOffset = -fmin * transferFunctionScale
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
        //signal?.let { logger.info("got a ${signal::class.simpleName} signal") }
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
                stageAreaCenter = (signal.stageMax + signal.stageMin).times(0.5f)
                stageRoot.spatial {
                    scale = Vector3f(signal.vertexDiameter / scaleDownFactor)
                    position = Vector3f(-1f) * stageAreaCenter * scale
                }
                stageAreaBorders.spatial {
                    position = stageAreaCenter
                    scale = (signal.stageMax - signal.stageMin).apply {
                        this.mul(1.05f)
                    }
                }

                focusFrame?.applyHardwareDimensions(signal)

            }
            is MicroscopeStatus -> {
                focusFrame?.spatial()?.position = signal.stagePosition
            }
            is Stack -> {
                val stack = signal
                val buffer =
                    MemoryUtil.memAlloc(
                        stack.size.x * stack.size.y * stack.size.z
                                * hardware.hardwareDimensions().numericType.bytes
                    )
                val volume = when (hardware.hardwareDimensions().numericType) {
                    NumericType.INT8 -> Volume.fromBuffer(
                        listOf(BufferedVolume.Timepoint("0", buffer)),
                        stack.size.x, stack.size.y, stack.size.z,
                        UnsignedByteType(),
                        hub, Vector3f(hardware.hardwareDimensions().vertexDiameter).toFloatArray()
                    )
                    NumericType.INT16 -> Volume.fromBuffer(
                        listOf(BufferedVolume.Timepoint("0", buffer)),
                        stack.size.x, stack.size.y, stack.size.z,
                        UnsignedShortType(),
                        hub, Vector3f(hardware.hardwareDimensions().vertexDiameter).toFloatArray()
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
            hwd.vertexDiameter,
            hwd.numericType.bytes,
            transferFunction,
            transferFunctionOffset,
            transferFunctionScale
        )
        node.spatial {
            position = signal.stagePos
            rotation = layout.sheetRotation()
        }

        val minDistance = hardware.hardwareDimensions().vertexDiameter
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
                from, to, hardware.hardwareDimensions().vertexDiameter
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


    fun userInteraction(inputHandler: InputHandler, cam: Camera) {
        listOf(
            "forward" to "G",
            "back" to "B",
            "left" to "V",
            "right" to "N",
            "up" to "C",
            "down" to "M"
        ).forEach { (name, key) ->
            inputHandler.addBehaviour(name, MovementCommand(name, { focusFrame }, cam, speed = 1f))
            inputHandler.addKeyBinding(name, key)
        }
        inputHandler.addBehaviour("snap", object : ClickBehaviour {
            override fun click(x: Int, y: Int) {
                snapSlice()
            }
        })
        inputHandler.addKeyBinding("snap", "T")
    }

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