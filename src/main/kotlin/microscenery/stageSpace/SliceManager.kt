package microscenery.stageSpace

import graphics.scenery.*
import graphics.scenery.utils.LazyLogger
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.toFloatArray
import graphics.scenery.volumes.BufferedVolume
import graphics.scenery.volumes.Volume
import microscenery.MicroscenerySettings
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


class SliceManager(val hardware : MicroscopeHardware, val stageRoot : RichNode, val scene : Scene) {
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))


    internal val sortedSlices = ArrayList<SliceRenderNode>()
    internal val sortingSlicesLock = ReentrantLock()

    internal var stacks = emptyList<StackContainer>()


    val transferFunctionManager = TransferFunctionManager(this)

    init {
        MicroscenerySettings.setIfUnset("Stage.CameraDependendZSorting", true)

        MicroscenerySettings.setIfUnset("Stage.ToggleSliceBorder", false)
        MicroscenerySettings.addUpdateRoutine("Stage.ToggleSliceBorder",
            {
                setSliceBorderVisibility(MicroscenerySettings.get("Stage.ToggleSliceBorder"))
            })

        val cam = scene.findObserver()
        if(cam != null)
        {
            var oldPos = cam.spatial().position

            cam.update += {
                if(MicroscenerySettings.get("Stage.CameraDependendZSorting") && oldPos != cam.spatial().position) {
                    sortAndInsertSlices(cam.spatial().position)
                    oldPos = cam.spatial().position
                }
            }
        }
    }

    /**
     * Sets the slice border visibility according to [visibility]
     */
    fun setSliceBorderVisibility(visibility : Boolean)
    {
        sortedSlices.forEach { it
            it.setBorderVisibility(visibility)
        }
    }

    fun sortAndInsertSlices(camPosition: Vector3f, newSlice: SliceRenderNode? = null){
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

    fun handleSliceSignal(sliceSignal : Slice, layout: MicroscopeLayout) {
        if (sliceSignal.data == null) return

        if (sliceSignal.stackIdAndSliceIndex != null && stacks.any { it.meta.Id == sliceSignal.stackIdAndSliceIndex!!.first }) {
            // slice belongs to a stack
            handleStackSlice(sliceSignal)
            return
        }
        // this slice does not belong to a stack and should be visualised on its own
        handleSingleSlice(sliceSignal, layout)
    }

    fun handleStackSignal(stackSignal : Stack, hub : Hub) {
        val stack = stackSignal
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
        volume.transferFunction = transferFunctionManager.transferFunction
        volume.name = "Stack ${stackSignal.Id}"
        volume.origin = Origin.Center
        volume.spatial().position = (stackSignal.from + stackSignal.to).mul(0.5f)
        volume.spatial().scale = Vector3f(1f, -1f, sliceThickness)
        volume.pixelToWorldRatio = 1f // conversion is done by stage root
        volume.setTransferFunctionRange(transferFunctionManager.minDisplayRange, transferFunctionManager.maxDisplayRange)

        stageRoot.addChild(volume)

        BoundingGrid().apply {
            this.node = volume
            //volume.metadata["BoundingGrid"] = this
        }
        stacks = stacks + StackContainer(stack, volume, buffer)
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


        stack.buffer.position(slice.size * sliceIndex)
        stack.buffer.put(slice.data)
        stack.buffer.rewind()
        stack.volume.goToNewTimepoint(stack.buffer)
    }

    private fun handleSingleSlice(signal : Slice, layout: MicroscopeLayout) {
        if (signal.data == null) return
        val hwd = hardware.hardwareDimensions()

        val node = SliceRenderNode(
            signal.data!!,
            hwd.imageSize.x,
            hwd.imageSize.y,
            1f,
            hwd.numericType.bytes,
            transferFunctionManager.transferFunction,
            transferFunctionManager.transferFunctionOffset,
            transferFunctionManager.transferFunctionScale
        )
        node.spatial {
            position = signal.stagePos
            rotation = layout.sheetRotation()
        }

        sortAndInsertSlices(scene.findObserver()?.spatial()?.position?:Vector3f(), node)
    }

    fun clearSlices() {
        sortedSlices.forEach {
            it.parent?.removeChild(it)
        }
        sortedSlices.clear()
        val tmp = stacks
        stacks = emptyList()
        tmp.forEach {
            it.volume.parent?.removeChild(it.volume)
            MemoryUtil.memFree(it.buffer)
        }
    }



    internal class StackContainer(val meta: Stack, val volume: BufferedVolume, val buffer: ByteBuffer)
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