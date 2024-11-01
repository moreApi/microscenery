package microscenery.stageSpace

import graphics.scenery.Hub
import graphics.scenery.SceneryElement
import graphics.scenery.backends.Renderer
import graphics.scenery.volumes.*
import microscenery.MicrosceneryHub
import microscenery.MicroscenerySettings
import microscenery.UI.UIModel
import org.jfree.data.statistics.SimpleHistogramDataset

class TransferFunctionManager(val sliceManager: SliceManager, val msHub: MicrosceneryHub) : HasTransferFunction, HasHistogram, HasColormap {

    override var minDisplayRange: Float = MicroscenerySettings.get("TransferFunction.DisplayRangeMin",0f)
        set(value) {
            field = value
            updateTransferFunction()
        }
    override var maxDisplayRange: Float  = MicroscenerySettings.get("TransferFunction.DisplayRangeMax",Short.MAX_VALUE.toFloat())
        set(value) {
            field = value
            updateTransferFunction()
        }
    override var transferFunction: TransferFunction = TransferFunction.ramp(0.0f, 1.0f, 0.5f)
        set(value) {
            field = value
            updateTransferFunction()
        }

    override var colormap = Colormap.get("hot")
        set(value) {
            field = value
            updateColorMap()
        }

    override var range: Pair<Float, Float> = 0.0f to Short.MAX_VALUE.toFloat()

    init {
        updateTransferFunction()
        updateColorMap()
    }

    /**
     * Iterates over all slices and stacks and updates their transferFunction, offset and scale values according
     * to the currently set values of this manager
     */
    private fun updateTransferFunction() {
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

    private fun updateColorMap() {
        sliceManager.sortedSlices.forEach {
            it.colormap = colormap
        }
        sliceManager.stacks.forEach {
            it.volume.colormap = colormap
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