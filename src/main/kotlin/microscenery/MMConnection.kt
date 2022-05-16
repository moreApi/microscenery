package microscenery

import MicroscenerySettings
import graphics.scenery.utils.LazyLogger
import let
import microscenery.hardware.SPIMSetup
import mmcorej.CMMCore
import java.awt.Rectangle
import java.nio.ShortBuffer

/**
 * Connection to MicroManger Core. Does the imaging.
 *
 * RelevantProperties:
 * MMConnection.core.configuration
 * MMConnection.core.settingsGroupName
 * MMConnection.core.presetName
 * MMConnection.minZ
 * MMConnection.maxZ
 * MMConnection.slices
 */
class MMConnection(
    core_: CMMCore? = null
)
{
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private val core: CMMCore
    private val setup: SPIMSetup

    var width: Int = 0
    var height: Int = 0

    var minZ: Double = MicroscenerySettings.get("MMConnection.minZ",0.0)
    var maxZ: Double = MicroscenerySettings.get("MMConnection.maxZ",10.0)
    var steps: Int = MicroscenerySettings.get("MMConnection.slices",10)

    var snapTimes = listOf<Long>()
    var copyTimes = listOf<Long>()

    @Suppress("unused")
    val meanSnapTime get() = if(snapTimes.isNotEmpty()) snapTimes.sum()/snapTimes.size else 0
    @Suppress("unused")
    val meanCopyTime get() = if(copyTimes.isNotEmpty())copyTimes.sum()/copyTimes.size else 0

    init {

        if (core_ != null){
            core = core_
        } else {
            //init core from properties
            core = CMMCore()

            val info = core.versionInfo
            println(info)

            val mmConfiguration = MicroscenerySettings.get<String>("MMConnection.core.configuration")
            core.loadSystemConfiguration(mmConfiguration)

            val mmSettingsGroupName = MicroscenerySettings.getOrNull<String>("MMConnection.core.settingsGroupName")
            val mmPresetName = MicroscenerySettings.getOrNull<String>("MMConnection.core.presetName")
            mmSettingsGroupName?.let(mmSettingsGroupName) { _, _ ->
                logger.info("Setting $mmSettingsGroupName to $mmPresetName")
                core.setConfig(mmSettingsGroupName, mmPresetName)
            }
        }


        setup = SPIMSetup.createDefaultSetup(core)

        updateSize()
    }

    fun updateSize(){
        setup.snapImage() // do this so the following parameters are set
        width = core.imageWidth.toInt()
        height = core.imageHeight.toInt()
    }

    fun updateParamters(){
        minZ = MicroscenerySettings.get("MMConnection.minZ",0.0)
        maxZ = MicroscenerySettings.get("MMConnection.maxZ",10.0)
        steps = MicroscenerySettings.get("MMConnection.slices",10)
    }

    fun setRoi(roi: Rectangle){
        core.setROI(roi.x,roi.y,roi.width,roi.height)
    }

    fun captureStack(intoBuffer: ShortBuffer) {
        var offset = 0
        var snapTime = 0L
        var copyTime = 0L

        val range = maxZ - minZ
        if (range <= 0)
            throw IllegalArgumentException("MaxZ needs to be larger thank MinZ.")
        val stepSize = range / steps

        (0 until steps).forEach { step ->
            val z = minZ + stepSize * step
            //core.snapImage()
            val start = System.currentTimeMillis()
            setup.zStage.position = z
            val img = setup.snapImage()
            snapTime += (System.currentTimeMillis()-start)
            //val img1 = core.image as ShortArray// returned as a 1D array of signed integers in row-major order
            //val sa = core.image as ShortArray

            val start2 = System.currentTimeMillis()
            val sa = img.pix as ShortArray
            sa.forEach {
                intoBuffer.put(offset, it)
                offset += 1
            }
            copyTime += (System.currentTimeMillis()-start2)
        }
        logger.info("$steps slices from $minZ to $maxZ took snap $snapTime ms copy $copyTime ms")
        recordTimes(snapTime,copyTime)
    }

    private fun recordTimes(snap: Long, copy: Long){
        snapTimes = snapTimes + (snap)
        while (snapTimes.size > 10)
            snapTimes = snapTimes.subList(1,snapTimes.size)
        copyTimes = copyTimes + (copy)
        while (copyTimes.size > 10)
            copyTimes = copyTimes.subList(1,copyTimes.size)
    }
}
