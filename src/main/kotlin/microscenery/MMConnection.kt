package microscenery

import graphics.scenery.utils.LazyLogger
import microscenery.hardware.SPIMSetup
import mmcorej.CMMCore
import java.nio.ShortBuffer

/**
 *
 * @param slices Should be not divisible by 32, otherwise the animation will be a standing wave.
 */
class MMConnection(
    private val slices: Int = 112,
    mmConfiguration: String = "C:/Program Files/Micro-Manager-2.0gamma/MMConfig_fake.cfg",
    mmSettingsGroupName: String = "FakeCam",
    mmPresetName: String = "TiffStack_16_Cherry_time" )
{
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))
    private val core = CMMCore()
    private val setup: SPIMSetup

    val width: Int
    val height: Int

    init {
        val info = core.versionInfo
        println(info)

        core.loadSystemConfiguration(mmConfiguration)
        core.setConfig(mmSettingsGroupName, mmPresetName)
        setup = SPIMSetup.createDefaultSetup(core)

        core.snapImage() // do this so the following parameters are set
        width = core.imageWidth.toInt()
        height = core.imageHeight.toInt()
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
}