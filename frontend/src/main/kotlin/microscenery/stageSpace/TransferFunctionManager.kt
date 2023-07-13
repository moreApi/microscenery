package microscenery.stageSpace

import graphics.scenery.volumes.HasTransferFunction
import graphics.scenery.volumes.TransferFunction
import microscenery.MicroscenerySettings
import microscenery.signals.NumericType

class TransferFunctionManager(val sliceManager: SliceManager) : HasTransferFunction {

    internal var transferFunctionOffset = 0.0f
    internal var transferFunctionScale = 1.0f

    override var minDisplayRange: Float = MicroscenerySettings.get("TransferFunction.DisplayRangeMin",0f)
        set(value) {
            field = value
            calculateOffsetAndScale()
            updateTransferFunction()
        }
    override var maxDisplayRange: Float  = MicroscenerySettings.get("TransferFunction.DisplayRangeMax",1000.0f)
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

    override var range: Pair<Float, Float> = 0.0f to 1000f

    init {
        calculateOffsetAndScale()
        updateTransferFunction()
    }

    /**
     * Iterates over all slices and stacks and updates their transferFunction, offset and scale values according
     * to the currently set values of this manager
     */
    internal fun updateTransferFunction() {
        sliceManager.sortedSlices.forEach {
            it.transferFunction = transferFunction
            it.transferFunctionOffset = transferFunctionOffset
            it.transferFunctionScale = transferFunctionScale
        }
        sliceManager.stacks.forEach {
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
        val rangeScale = when (sliceManager.hardware.hardwareDimensions().numericType) {
            NumericType.INT8 -> 255
            NumericType.INT16 -> 65535
        }
        val fmin = minDisplayRange / rangeScale
        val fmax = maxDisplayRange / rangeScale
        transferFunctionScale = 1.0f / (fmax - fmin)
        transferFunctionOffset = -fmin * transferFunctionScale
    }

}