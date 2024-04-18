package microscenery.stageSpace

import graphics.scenery.volumes.HasTransferFunction
import graphics.scenery.volumes.TransferFunction
import microscenery.MicroscenerySettings
import microscenery.signals.NumericType

class TransferFunctionManager(val sliceManager: SliceManager) : HasTransferFunction {

    override var minDisplayRange: Float = MicroscenerySettings.get("TransferFunction.DisplayRangeMin",0f)
        set(value) {
            field = value
            updateTransferFunction()
        }
    override var maxDisplayRange: Float  = MicroscenerySettings.get("TransferFunction.DisplayRangeMax",1000.0f)
        set(value) {
            field = value
            updateTransferFunction()
        }
    override var transferFunction: TransferFunction = TransferFunction.ramp(0.0f, 1.0f, 0.5f)
        set(value) {
            field = value
            updateTransferFunction()
        }

    override var range: Pair<Float, Float> = 0.0f to 10000f

    init {
        updateTransferFunction()
    }

    /**
     * Iterates over all slices and stacks and updates their transferFunction, offset and scale values according
     * to the currently set values of this manager
     */
    internal fun updateTransferFunction() {
        sliceManager.sortedSlices.forEach {
            it.transferFunction = transferFunction
            it.minDisplayRange = minDisplayRange
            it.maxDisplayRange = maxDisplayRange
        }
        sliceManager.stacks.forEach {
            it.volume.transferFunction = transferFunction
            it.volume.minDisplayRange = minDisplayRange
            it.volume.maxDisplayRange = maxDisplayRange
        }
    }
}