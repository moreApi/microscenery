package microscenery.stageSpace

import graphics.scenery.*
import graphics.scenery.attribute.material.Material
import graphics.scenery.utils.LazyLogger
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import graphics.scenery.utils.extensions.toFloatArray
import graphics.scenery.volumes.BufferedVolume
import graphics.scenery.volumes.HasTransferFunction
import graphics.scenery.volumes.TransferFunction
import graphics.scenery.volumes.Volume
import microscenery.Agent
import microscenery.MicroscenerySettings
import microscenery.hardware.MicroscopeHardware
import microscenery.signals.*
import net.imglib2.type.numeric.integer.UnsignedByteType
import net.imglib2.type.numeric.integer.UnsignedShortType
import org.joml.Vector2f
import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil
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
    val scaleDownFactor: Float = 100f,
    val layout: MicroscopeLayout = MicroscopeLayout.Default()
) : Agent(), HasTransferFunction {
    internal val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))


    val stageRoot = RichNode("stage root")
    val focus: Frame
    var focusTarget: FocusFrame? = null
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
            updateTransferFunction()
        }
    override var maxDisplayRange: Float = 1000.0f
        set(value) {
            field = value
            calculateOffsetAndScale()
            updateTransferFunction()
        }
    override var transferFunction: TransferFunction = TransferFunction.ramp(0.0f, 1.0f, 0.5f)
        set(value) {
            field = value
            updateTransferFunction()
        }

    init {
        MicroscenerySettings.setIfUnset("Stage.ExploreResolutionX", 10f)
        MicroscenerySettings.setIfUnset("Stage.ExploreResolutionY", 10f)
        MicroscenerySettings.setIfUnset("Stage.ExploreResolutionZ", 10f)

        scene.addChild(stageRoot)
        calculateOffsetAndScale()

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


        focus = Frame(hardware.hardwareDimensions(), Vector3f(0.4f, 0.4f, 1f)).apply {
            spatial().position = hardware.stagePosition
            stageRoot.addChild(this)
        }

        if (addFocusFrame)
            focusTarget = FocusFrame(this, hardware.hardwareDimensions()).apply {
                spatial().position = hardware.stagePosition
                stageRoot.addChild(this)
            }

        focusTarget?.children?.first()?.spatialOrNull()?.rotation = layout.sheetRotation()
        focus.children.first()?.spatialOrNull()?.rotation = layout.sheetRotation()


        startAgent()
    }

    /**
     * Iterates over all slices and stacks and updates their transferFunction, offset and scale values according
     * to the currently set values of this manager
     */
    private fun updateTransferFunction() {
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
     * This normally happens inside the converter of a volume.
     * Converts the minDisplayRange and maxDisplayRange values into an offset and scale used inside the shader
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
        signal?.let { logger.info("got a ${signal::class.simpleName} signal:\n$signal") }
        when (signal) {
            is Slice -> {
                if (signal.data == null) return

                if (signal.stackIdAndSliceIndex != null && stacks.any { it.meta.Id == signal.stackIdAndSliceIndex.first }) {
                    // slice belongs to a stack
                    handleStackSlice(signal)
                    return
                }
                // this slice does not belong to a stack and should be visualised on its own
                handleSingleSlice(signal)
            }
            is HardwareDimensions -> {
                logger.info("Got HWD:$signal")
                stageAreaCenter = (signal.stageMax + signal.stageMin).times(0.5f)
                stageRoot.spatial {
                    scale = Vector3f((1 / scaleDownFactor) * signal.vertexDiameter)
                    position = Vector3f(-1f) * stageAreaCenter * scale
                }
                stageAreaBorders.spatial {
                    position = stageAreaCenter
                    scale = (signal.stageMax - signal.stageMin).apply {
                        // extra space for images at the edge of stage space
                        val imgPixSize = Vector2f(signal.imageSize)
                        val imageSize = when (layout.sheet) {
                            MicroscopeLayout.Axis.X -> Vector3f(0f, imgPixSize.y, imgPixSize.x)
                            MicroscopeLayout.Axis.Y -> Vector3f(imgPixSize.x, 0f, imgPixSize.y)
                            MicroscopeLayout.Axis.Z -> Vector3f(imgPixSize, 0f)
                        }
                        this.add(imageSize)
                        this.mul(1.02f)
                    }
                }

                focusTarget?.applyHardwareDimensions(signal)
                focus.applyHardwareDimensions(signal)

            }
            is MicroscopeStatus -> {
                focus.spatial().position = signal.stagePosition
            }
            is Stack -> {
                val stack = signal
                val x = hardware.hardwareDimensions().imageSize.x
                val y = hardware.hardwareDimensions().imageSize.y
                val z = stack.slicesCount
                val sliceThickness = (stack.to.z - stack.from.z) / stack.slicesCount
                val buffer = MemoryUtil.memAlloc(
                    x * y * z * hardware.hardwareDimensions().numericType.bytes
                )
                val volume = when (hardware.hardwareDimensions().numericType) {
                    NumericType.INT8 -> Volume.fromBuffer(
                        listOf(BufferedVolume.Timepoint("0", buffer)),
                        x,
                        y,
                        z,
                        UnsignedByteType(),
                        hub,
                        Vector3f(1f, 1f, sliceThickness).toFloatArray()// conversion is done by stage root
                    )
                    NumericType.INT16 -> Volume.fromBuffer(
                        listOf(BufferedVolume.Timepoint("0", buffer)),
                        x,
                        y,
                        z,
                        UnsignedShortType(),
                        hub,
                        Vector3f(1f, 1f, sliceThickness).toFloatArray()// conversion is done by stage root
                    )
                }
                volume.goToLastTimepoint()
                volume.transferFunction = transferFunction
                volume.name = "Stack ${signal.Id}"
                volume.origin = Origin.Center
                volume.spatial().position = (signal.from + signal.to).mul(0.5f)
                volume.spatial().scale = Vector3f(1f, -1f, sliceThickness)
                volume.pixelToWorldRatio = 1f // conversion is done by stage root
                volume.setTransferFunctionRange(minDisplayRange, maxDisplayRange)

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
        val stackId = slice.stackIdAndSliceIndex?.first ?: return
        val sliceIndex = slice.stackIdAndSliceIndex.second

        val stack = stacks.find { it.meta.Id == stackId }
        if (stack == null) {
            logger.error("Did not find stack id: ${stackId}")
            return
        }


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
                signal.stagePos, minDistance
            ) ?: false
        }.toList() // get out of children.iterator or something, might be bad to do manipulation within an iterator
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

    fun stack(from: Vector3f, to: Vector3f, live: Boolean) {
        hardware.acquireStack(
            ClientSignal.AcquireStack(
                from,
                to,
                MicroscenerySettings.get("Stage.precisionZ", hardware.hardwareDimensions().vertexDiameter),
                live
            )
        )
    }

    fun snapSlice() {
        hardware.snapSlice()
    }

    fun goLive() {
        logger.info("going live")
        hardware.goLive()
    }

    fun stop() {
        logger.info("stopping")
        hardware.stop()
    }

    fun exploreCubeStageSpace(p1: Vector3f, p2: Vector3f, resolution: Vector3f = Vector3f(
        MicroscenerySettings.get("Stage.ExploreResolutionX", 10f),
        MicroscenerySettings.get("Stage.ExploreResolutionY", 10f),
        MicroscenerySettings.get("Stage.ExploreResolutionZ", 10f)
    )) {
        if (hardware.status().state != ServerState.MANUAL) {
            throw IllegalStateException("Can only start sampling stage space if server is in Manual state.")
        }

        val from = Vector3f(
            p1.x.coerceAtMost(p2.x),
            p1.y.coerceAtMost(p2.y),
            p1.z.coerceAtMost(p2.z),
        )
        val to = Vector3f(
            p1.x.coerceAtLeast(p2.x),
            p1.y.coerceAtLeast(p2.y),
            p1.z.coerceAtLeast(p2.z),
        )

        val positions = mutableListOf<Vector3f>()
        // I'm missing classic for loops, kotlin :,(
        var x = from.x
        while (x <= to.x) {
            var y = from.y
            while (y <= to.y) {
                var z = from.z
                while (z <= to.z) {
                    positions += Vector3f(x, y, z)
                    z += resolution.z
                }
                y += resolution.y
            }
            x += resolution.x
        }

        positions.forEach {
            this.stagePosition = it
            this.snapSlice()
        }
    }

    private class StackContainer(val meta: Stack, val volume: BufferedVolume, val buffer: ByteBuffer)


    companion object {
        private fun BufferedVolume.goToNewTimepoint(buffer: ByteBuffer) {
            val volume = this
            volume.lock.withLock {
                val count = volume.timepoints?.lastOrNull()?.name?.toIntOrNull() ?: 0
                volume.addTimepoint("${count + 1}", buffer)
                //logger.info("going to Timepoint ${volume.goToLastTimepoint()}")
                volume.purgeFirst(3, 3)
            }
        }
    }
}