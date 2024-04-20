package microscenery.stageSpace

import graphics.scenery.Hub
import graphics.scenery.SceneryElement
import graphics.scenery.backends.Renderer
import graphics.scenery.volumes.HasHistogram
import graphics.scenery.volumes.HasTransferFunction
import graphics.scenery.volumes.TransferFunction
import graphics.scenery.volumes.Volume
import microscenery.MicrosceneryHub
import microscenery.MicroscenerySettings
import microscenery.UI.UIModel
import org.jfree.data.statistics.SimpleHistogramDataset

class TransferFunctionManager(val sliceManager: SliceManager, val msHub: MicrosceneryHub) : HasTransferFunction, HasHistogram {

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

    override fun generateHistogram(volumeHistogramData: SimpleHistogramDataset): Int? {
        val uiModel = msHub.getAttributeOrNull(UIModel::class.java)
        val render = msHub.getAttribute(Hub::class.java).get<Renderer>(SceneryElement.Renderer) ?: return null

        val vol = uiModel?.selected as? Volume ?: sliceManager.stacks.firstOrNull()?.volume
        val slice = uiModel?.selected as? SliceRenderNode ?: sliceManager.sortedSlices.firstOrNull()

        return vol?.generateHistogram(volumeHistogramData) ?: slice?.generateHistogram(volumeHistogramData,render)
    }
}