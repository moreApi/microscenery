package microscenery.stageSpace

import graphics.scenery.volumes.HasTransferFunction
import graphics.scenery.volumes.TransferFunction
import microscenery.signals.NumericType

class TransferFunctionManager(val stageSpace: StageSpaceManager): HasTransferFunction {

    internal var transferFunctionOffset = 0.0f
    internal var transferFunctionScale = 1.0f

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

    /**
     * Iterates over all slices and stacks and updates their transferFunction, offset and scale values according
     * to the currently set values of this manager
     */
    private fun updateTransferFunction() {
        stageSpace.sortedSlices.forEach {
            it.transferFunction = transferFunction
            it.transferFunctionOffset = transferFunctionOffset
            it.transferFunctionScale = transferFunctionScale
        }
        stageSpace.stacks.forEach {
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
        val rangeScale = when (stageSpace.hardware.hardwareDimensions().numericType) {
            NumericType.INT8 -> 255
            NumericType.INT16 -> 65535
        }
        val fmin = minDisplayRange / rangeScale
        val fmax = maxDisplayRange / rangeScale
        transferFunctionScale = 1.0f / (fmax - fmin)
        transferFunctionOffset = -fmin * transferFunctionScale
    }

}