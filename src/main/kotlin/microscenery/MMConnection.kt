package microscenery

import GlobalSettings
import graphics.scenery.utils.LazyLogger
import graphics.scenery.utils.RingBuffer
import let
import microscenery.hardware.SPIMSetup
import mmcorej.CMMCore
import org.lwjgl.system.MemoryUtil
import java.awt.Rectangle
import java.nio.ByteBuffer
import java.nio.ShortBuffer

/**
 *
 * @param slices Should be not divisible by 32, otherwise the animation will be a standing wave.
 */
class MMConnection(
    var slices: Int = GlobalSettings.get("MMConnection.slices"),
    core_: CMMCore? = null
)
{
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private val core: CMMCore
    private val setup: SPIMSetup

    var width: Int = 0
    var height: Int = 0

    init {

        if (core_ != null){
            core = core_
        } else {
            //init core from properties
            core = CMMCore()

            val info = core.versionInfo
            println(info)

            val mmConfiguration = GlobalSettings.get<String>("MMConnection.core.configuration")
            core.loadSystemConfiguration(mmConfiguration)

            val mmSettingsGroupName = GlobalSettings.getOrNull<String>("MMConnection.core.settingsGroupName")
            val mmPresetName = GlobalSettings.getOrNull<String>("MMConnection.core.presetName")
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

    fun setRoi(roi: Rectangle){
        core.setROI(roi.x,roi.y,roi.width,roi.height)
    }

    fun captureStack(intoBuffer: ShortBuffer, minZ: Double = 0.0, maxZ: Double = slices.toDouble(), steps: Int = slices) {
        var offset = 0
        var snap = 0L
        var copy = 0L

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
            snap += (System.currentTimeMillis()-start)
            //val img1 = core.image as ShortArray// returned as a 1D array of signed integers in row-major order
            //val sa = core.image as ShortArray

            val start2 = System.currentTimeMillis()
            val sa = img.pix as ShortArray
            sa.forEach {
                intoBuffer.put(offset, it)
                offset += 1
            }
            copy += (System.currentTimeMillis()-start2)

        }
        logger.info("slices $slices snap $snap ms copy $copy ms")

    }

    companion object{
        @JvmStatic
        fun main(args: Array<String>) {

            val slices = 112
            val mmConnection = MMConnection(slices)


            val volumeBuffer =
                RingBuffer<ByteBuffer>(2, default = {
                    MemoryUtil.memAlloc((mmConnection.width * mmConnection.height * slices * Short.SIZE_BYTES))
                })

            val start = System.currentTimeMillis()
            for (x in 1..10){
                val currentBuffer = volumeBuffer.get()
                mmConnection.captureStack(currentBuffer.asShortBuffer())
            }
            println("took ${System.currentTimeMillis()-start}ms")


        }
    }
}
