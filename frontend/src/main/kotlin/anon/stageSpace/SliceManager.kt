package anon.stageSpace

import fromScenery.utils.extensions.minus
import graphics.scenery.*
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import graphics.scenery.utils.extensions.toFloatArray
import graphics.scenery.utils.lazyLogger
import graphics.scenery.volumes.BufferedVolume
import graphics.scenery.volumes.Colormap
import graphics.scenery.volumes.Volume
import anon.MicrosceneryHub
import anon.MicroscenerySettings
import anon.Settings
import anon.UI.UIModel
import anon.detach
import anon.hardware.MicroscopeHardware
import anon.signals.NumericType
import anon.signals.Slice
import anon.signals.Stack
import net.imglib2.type.numeric.integer.UnsignedByteType
import net.imglib2.type.numeric.integer.UnsignedShortType
import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.sqrt
import kotlin.random.Random


class SliceManager(val hardware: MicroscopeHardware, val stageRoot: RichNode, val scene: Scene, val msHub: MicrosceneryHub) {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))


    internal val sortingSlicesLock = ReentrantLock()

    internal val sortedSlices = ArrayList<SliceRenderNode>()
    internal var stacks = emptyList<StackContainer>()
    internal var selectedStack: StackContainer? = null

    val transferFunctionManager = TransferFunctionManager(this, msHub)

    private val flipVector: Vector3f
        get() {return Vector3f(
            if (MicroscenerySettings.get("Stage.Image.FlipX",false)) -1f else 1f,
            if (MicroscenerySettings.get("Stage.Image.FlipY",false)) -1f else 1f,
            if (MicroscenerySettings.get("Stage.Image.FlipZ",false)) -1f else 1f)}

    init {
        MicroscenerySettings.setIfUnset(Settings.StageSpace.CameraDependendZSorting, true)

        MicroscenerySettings.setIfUnset("Stage.Image.FlipX", false)
        MicroscenerySettings.setIfUnset("Stage.Image.FlipY", false)
        MicroscenerySettings.setIfUnset("Stage.Image.FlipZ", false)

        MicroscenerySettings.setIfUnset("Stage.ToggleSliceBorder", false)
        MicroscenerySettings.addUpdateRoutine(
            "Stage.ToggleSliceBorder"
        ) { setSliceBorderVisibility(MicroscenerySettings.get("Stage.ToggleSliceBorder",false)) }
        MicroscenerySettings.addUpdateRoutine(Settings.StageSpace.ColorMap){
            val color = getColorMap() ?: return@addUpdateRoutine
            stacks.forEach { it.volume.colormap = color }
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
            msHub.getAttributeOrNull(UIModel::class.java)?.updateSelected()
        } else {

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
            volume.colormap = transferFunctionManager.colormap
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
            msHub.getAttributeOrNull(UIModel::class.java)?.selected = stacks.last().volume
        }
    }

    private fun getColorMap(): Colormap? {
        val colorName = MicroscenerySettings.getOrNull<String>(Settings.StageSpace.ColorMap) ?: return null
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

    private var camUpdateLambda: (() -> Unit)? = null

    /**
     * Set up the painters algorithm routine for the camera if not already setup.
     * We cant do that at initialization because then the camera might not be initialized.
     */
    private fun setupCameraPainterAlgorithmUpdate(){
        if (camUpdateLambda == null){
            val cam = scene.findObserver()
            if (cam == null) {
                logger.warn("No cam found, cant setup painters algorithm for slices.")
                return
            }
            var oldRotationHash = cam.spatial().rotation.hashCode()
            camUpdateLambda = {
                if (MicroscenerySettings.get("Stage.CameraDependendZSorting",true) && oldRotationHash != cam.spatial().rotation.hashCode()) {
                    sortSlices(cam)
                    oldRotationHash = cam.spatial().rotation.hashCode()
                }
            }
            cam.update.add(camUpdateLambda!!)
        }
    }

    private fun sortAndInsertSlices(cam: Camera, newSlice: SliceRenderNode) {
        // detect too close slices to replace them
        sortingSlicesLock.withLock {
            stageRoot.children.filter {
                it is SliceRenderNode && it.spatialOrNull()?.position
                    ?.equals(
                        newSlice.spatial().position,
                        hardware.hardwareDimensions().vertexDiameter
                ) ?: false
            }.toList() // get out of children.iterator or something, might be bad to do manipulation within an iterator
                .forEach {
                    stageRoot.removeChild(it)
                    sortedSlices.remove(it)
                }
        }
        //insert slice
        sortSlices(cam, newSlice)
    }

    /**
     * Sorts slices with an adjusted painters algorithm.
     * This causes the slices to be rendered in a back to front order so that their transparency is displayed correctly.
     */
    private fun sortSlices(cam: Camera, newSlice: SliceRenderNode? = null) {
        setupCameraPainterAlgorithmUpdate()

        sortingSlicesLock.withLock {
            newSlice?.let {
                sortedSlices.add(it)
                stageRoot.addChild(it)
                it.spatial().updateWorld(false,true)
            }
            if (sortedSlices.isEmpty()) return
            
            // Adjusted Painters algorithm for uniformly leaning planes
            // General Idea: sort the planes roughly along their normal

            val sliceNormal = Vector3f(0f,0f,1f).rotate(sortedSlices[0].spatial().worldRotation())
            val camNormal = Vector3f(0f,0f,1f).rotate(cam.spatial().worldRotation())

            // multiply normal with a lage factor to serve as a point at "infinity" in world space.
            // if the camera view direction aligns with the normal invert it
            val normalDiff = (camNormal - sliceNormal).length()
            val camAndNormalInSameDirection = normalDiff < sqrt(2f)
            val sliceNormalInv = sliceNormal * 100000f * if(camAndNormalInSameDirection) -1f else 1f

            // Sorts the [sortedSlices] container using the distance between "infinite normal point"
            // and slice in order (the clostest first).
            sortedSlices.sortBy { it.spatial().worldPosition().distance(sliceNormalInv) }
            // remove and add in new order to get correct render ordering
            sortedSlices.forEach {
                stageRoot.children.remove(it)
            }
            sortedSlices.forEach {
                stageRoot.children.add(it)
            }
            // Resulting in a back to front rendering
            // Possible improvement: instead of using the distance to a point at infinite,
            // project on a line spanned by the normal
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
            transferFunctionManager.minDisplayRange,
            transferFunctionManager.maxDisplayRange,
            transferFunctionManager.colormap
        )
        // random offset might be set to avoid z-fighting for close slices
        val randomOffset = MicroscenerySettings.getOrNull<Float>(Settings.StageSpace.RandomSliceOffset)
        node.spatial {
            position = signal.stagePos + (randomOffset?.let { Vector3f(it*Random.nextFloat())} ?: Vector3f(0f))
            rotation = layout.sheetRotation()
            scale *= flipVector
        }

        scene.findObserver()?.let { sortAndInsertSlices(it,node) }
    }

    /**
     * @param currentBuffer points to the buffer of the most recent timepoint. It might be not completly filled
     */
    internal class StackContainer(val meta: Stack, val volume: BufferedVolume, var currentBuffer: ByteBuffer)
}