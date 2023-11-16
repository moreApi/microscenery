package microscenery.stageSpace

import graphics.scenery.Hub
import graphics.scenery.Origin
import graphics.scenery.RichNode
import graphics.scenery.Scene
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import graphics.scenery.utils.extensions.toFloatArray
import graphics.scenery.utils.lazyLogger
import graphics.scenery.volumes.BufferedVolume
import graphics.scenery.volumes.Colormap
import graphics.scenery.volumes.Volume
import microscenery.MicroscenerySettings
import microscenery.detach
import microscenery.hardware.MicroscopeHardware
import microscenery.signals.NumericType
import microscenery.signals.Slice
import microscenery.signals.Stack
import net.imglib2.type.numeric.integer.UnsignedByteType
import net.imglib2.type.numeric.integer.UnsignedShortType
import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


class SliceManager(val hardware: MicroscopeHardware, val stageRoot: RichNode, val scene: Scene) {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))


    private val sortingSlicesLock = ReentrantLock()

    internal val sortedSlices = ArrayList<SliceRenderNode>()
    internal var stacks = emptyList<StackContainer>()
    internal var selectedStack: StackContainer? = null

    val transferFunctionManager = TransferFunctionManager(this)

    private val flipVector: Vector3f
        get() {return Vector3f(
            if (MicroscenerySettings.get("Stage.Image.FlipX",false)) -1f else 1f,
            if (MicroscenerySettings.get("Stage.Image.FlipY",false)) -1f else 1f,
            if (MicroscenerySettings.get("Stage.Image.FlipZ",false)) -1f else 1f)}

    init {
        MicroscenerySettings.setIfUnset("Stage.CameraDependendZSorting", true)

        MicroscenerySettings.setIfUnset("Stage.Image.FlipX", false)
        MicroscenerySettings.setIfUnset("Stage.Image.FlipY", false)
        MicroscenerySettings.setIfUnset("Stage.Image.FlipZ", false)

        MicroscenerySettings.setIfUnset("Stage.ToggleSliceBorder", false)
        MicroscenerySettings.addUpdateRoutine(
            "Stage.ToggleSliceBorder"
        ) { setSliceBorderVisibility(MicroscenerySettings.get("Stage.ToggleSliceBorder")) }
        MicroscenerySettings.addUpdateRoutine(microscenery.Settings.StageSpace.ColorMap){
            val color = getColorMap() ?: return@addUpdateRoutine
            stacks.forEach { it.volume.colormap = color }
        }


        val cam = scene.findObserver()
        if (cam != null) {
            var oldPos = cam.spatial().position

            cam.update += {
                if (MicroscenerySettings.get("Stage.CameraDependendZSorting") && oldPos != cam.spatial().position) {
                    sortAndInsertSlices(cam.spatial().position)
                    oldPos = cam.spatial().position
                }
            }
        }
    }

    fun getStackMetadata(volume: Volume):Stack?{
        return stacks.firstOrNull { it.volume == volume }?.meta
    }

    fun deleteStack(volume: Volume){
        val stackContainer = stacks.firstOrNull { it.volume == volume }?:return
        stacks = stacks - stackContainer
        stackContainer.let {
            it.volume.parent?.removeChild(it.volume)
            it.volume.volumeManager.remove(it.volume)
            MemoryUtil.memFree(it.currentBuffer)
        }
    }

    fun deleteSlice(slice: SliceRenderNode){
        sortingSlicesLock.withLock {
            sortedSlices -= slice
            slice.detach()
        }
    }

    fun clearSlices() {
        sortingSlicesLock.withLock {
            sortedSlices.forEach {
                it.parent?.removeChild(it)
            }
            sortedSlices.clear()
        }

        val tmp = stacks
        stacks = emptyList()
        tmp.forEach {
            it.volume.parent?.removeChild(it.volume)
            it.volume.volumeManager.remove(it.volume)
            MemoryUtil.memFree(it.currentBuffer)
        }
    }


    fun handleSliceSignal(sliceSignal: Slice, layout: MicroscopeLayout) {
        if (sliceSignal.data == null) return

        if (sliceSignal.stackIdAndSliceIndex != null && stacks.any { it.meta.Id == sliceSignal.stackIdAndSliceIndex!!.first }) {
            // slice belongs to a stack
            handleStackSlice(sliceSignal)
            return
        }
        // this slice does not belong to a stack and should be visualised on its own
        handleSingleSlice(sliceSignal, layout)
    }

    fun handleStackSignal(stackSignal: Stack, hub: Hub) {
        val stack = stackSignal

        val x = hardware.hardwareDimensions().imageSize.x
        val y = hardware.hardwareDimensions().imageSize.y
        val z = stack.slicesCount
        val sliceThickness = (stack.to.z - stack.from.z) / stack.slicesCount
        val buffer = MemoryUtil.memAlloc(
            x * y * z * hardware.hardwareDimensions().numericType.bytes
        )

        // if there is an stack with the same Id add this one as a new timepoint to it, otherwise create a new stack.
        val existingStack = stacks.firstOrNull() { it.meta.Id == stack.Id }
        if (existingStack != null) {
            val volume = existingStack.volume
            volume.addTimepoint(stack.created.toString(),buffer)
            existingStack.currentBuffer = buffer
            volume.goToLastTimepoint()
        } else {
            // todo handle old stacks better
            selectedStack?.let {
                if (it.meta.Id != stack.Id) {
                    clearSlices()
                    selectedStack = null
                }
            }

            val volume = when (hardware.hardwareDimensions().numericType) {
                NumericType.INT8 -> Volume.fromBuffer(
                    listOf(BufferedVolume.Timepoint(stack.created.toString(), buffer)),
                    x,
                    y,
                    z,
                    UnsignedByteType(),
                    hub,
                    Vector3f(1f, 1f, sliceThickness).toFloatArray()// conversion is done by stage root
                )
                NumericType.INT16 -> Volume.fromBuffer(
                    listOf(BufferedVolume.Timepoint(stack.created.toString(), buffer)),
                    x,
                    y,
                    z,
                    UnsignedShortType(),
                    hub,
                    Vector3f(1f, 1f, sliceThickness).toFloatArray()// conversion is done by stage root
                )
            }
            volume.goToLastTimepoint()
            volume.transferFunction = transferFunctionManager.transferFunction
            getColorMap()?.let { volume.colormap = it }
            volume.name = "Stack ${stackSignal.Id}"
            volume.origin = Origin.Center
            volume.spatial {
                position = (stackSignal.from + stackSignal.to).mul(0.5f)
                scale = Vector3f(1f, -1f, sliceThickness)
                scale *= Vector3f(
                    hardware.hardwareDimensions().vertexDiameter,
                    hardware.hardwareDimensions().vertexDiameter,
                    1f
                )
                scale *= flipVector
            }
            volume.pixelToWorldRatio = 1f // conversion is done by stage root
            volume.setTransferFunctionRange(
                transferFunctionManager.minDisplayRange,
                transferFunctionManager.maxDisplayRange
            )

            stageRoot.addChild(volume)

            stacks = stacks + StackContainer(stack, volume, buffer)


            // todo: make stack selection smarter
            selectedStack = stacks.last()
        }
    }

    private fun getColorMap(): Colormap? {
        val colorName = MicroscenerySettings.getOrNull<String>(microscenery.Settings.StageSpace.ColorMap) ?: return null
        return try {
            Colormap.get(colorName)
        } catch (t: Throwable){
            logger.error("Could not find color $colorName")
            logger.debug(t.toString())
            null
        }
    }

    /**
     * Sets the slice border visibility according to [visibility]
     */
    private fun setSliceBorderVisibility(visibility: Boolean) {
        sortedSlices.forEach {
            it.setBorderVisibility(visibility)
        }
    }

    private fun sortAndInsertSlices(camPosition: Vector3f, newSlice: SliceRenderNode? = null) {
        if (newSlice != null) {
            sortingSlicesLock.lock()
            // detect too close slices to replace them
            stageRoot.children.filter {
                it is SliceRenderNode && it.spatialOrNull()?.position?.equals(
                    newSlice.spatial().position, hardware.hardwareDimensions().vertexDiameter
                ) ?: false
            }.toList() // get out of children.iterator or something, might be bad to do manipulation within an iterator
                .forEach {
                    stageRoot.removeChild(it)
                    sortedSlices.remove(it)
                }

            stageRoot.addChild(newSlice)
            sortedSlices.add(newSlice)
        }

        // If I have this lock that means I just inserted a slice and need to sort it. Otherwise, I look if it is free.
        // If yes I sort. If no I return because someone else is sorting (and inserting).
        if (sortingSlicesLock.isHeldByCurrentThread || sortingSlicesLock.tryLock()) {
            sortedSlices.forEach {
                stageRoot.children.remove(it)
            }

            //Sorts the [sortedSlices] container using the distance between camera and slice in descending order (the furthest first)
            sortedSlices.sortByDescending { it.spatial().worldPosition().distance(camPosition) }

            sortedSlices.forEach {
                stageRoot.children.add(it)
            }
            sortingSlicesLock.unlock()
        }

    }

    private fun handleStackSlice(slice: Slice) {
        if (slice.data == null) return
        val stackId = slice.stackIdAndSliceIndex?.first ?: return
        val sliceIndex = slice.stackIdAndSliceIndex!!.second

        val stack = stacks.find { it.meta.Id == stackId }
        if (stack == null) {
            logger.error("Did not find stack id: ${stackId}")
            return
        }

        val buf = stack.currentBuffer.duplicate()
        buf.position(slice.size * sliceIndex)
        buf.put(slice.data)
        buf.rewind()
        stack.volume.goToLastTimepoint()
        stack.volume.volumeManager.notifyUpdate(stack.volume)
    }

    private fun handleSingleSlice(signal: Slice, layout: MicroscopeLayout) {
        if (signal.data == null) return
        val hwd = hardware.hardwareDimensions()

        val node = SliceRenderNode(
            signal.data!!,
            hwd.imageSize.x,
            hwd.imageSize.y,
            hwd.vertexDiameter,
            hwd.numericType.bytes,
            transferFunctionManager.transferFunction,
            transferFunctionManager.transferFunctionOffset,
            transferFunctionManager.transferFunctionScale
        )
        node.spatial {
            position = signal.stagePos
            rotation = layout.sheetRotation()
            scale *= flipVector
        }

        sortAndInsertSlices(scene.findObserver()?.spatial()?.position ?: Vector3f(), node)
    }

    /**
     * @param currentBuffer points to the buffer of the most recent timepoint. It might be not completly filled
     */
    internal class StackContainer(val meta: Stack, val volume: BufferedVolume, var currentBuffer: ByteBuffer)
}