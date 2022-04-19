package microscenery

import getProperty
import getPropertyInt
import getPropertyString
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
    val slices: Int = getPropertyInt("MMConnection.slices"),
    core_ :CMMCore? = null)
{
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private val core: CMMCore
    private val setup: SPIMSetup

    val width: Int
    val height: Int

    init {

        if (core_ != null){
            core = core_
        } else {
            //init core from properties
            core = CMMCore()

            val info = core.versionInfo
            println(info)

            val mmConfiguration = getPropertyString("MMConnection.core.configuration")
            core.loadSystemConfiguration(mmConfiguration)

            val mmSettingsGroupName = getProperty("MMConnection.core.settingsGroupName")
            val mmPresetName = getProperty("MMConnection.core.presetName")
            mmSettingsGroupName?.let(mmSettingsGroupName){_, _ ->
                logger.info("Setting $mmSettingsGroupName to $mmPresetName")
                core.setConfig(mmSettingsGroupName, mmPresetName)
            }

            getProperty("MMConnection.core.exposure")?.let{
                val d = it.toDoubleOrNull()
                if (d == null){
                    logger.error("MMConnection.core.exposure is set to $it but could not be cast to double.")
                    return@let
                }
                core.exposure = d
            }

            getProperty("MMConnection.core.binning")?.let {
                core.setProperty("Camera", "Binning", it)
            }

            getProperty("MMConnection.core.roi")?.let {
                val v = it.trim().split(",").map { it.toInt() }.toList()
                setRoi(Rectangle(v[0],v[1],v[2],v[3]))
            }
        }


        setup = SPIMSetup.createDefaultSetup(core)

        core.snapImage() // do this so the following parameters are set
        width = core.imageWidth.toInt()
        height = core.imageHeight.toInt()
    }

    fun setRoi(roi: Rectangle){
        core.setROI(roi.x,roi.y,roi.width,roi.height)
    }

    fun captureStack(intoBuffer: ShortBuffer) {
        var offset = 0
        var snap = 0L
        var copy = 0L
        (0 until slices).forEach { z ->
            //core.snapImage()
            val start = System.currentTimeMillis()
            setup.zStage.position = z.toDouble()
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
