package microscenery.hardware.micromanagerConnection


import fromScenery.lazyLogger
import microscenery.MicroscenerySettings
import microscenery.Settings
import mmcorej.CMMCore
import org.joml.Vector3f
import java.awt.Rectangle
import java.nio.Buffer
import java.nio.ShortBuffer

/**
 * Connection to MicroManger Core. Does the imaging.
 *
 * RelevantProperties:
 * MMConnection.core.configuration
 * MMConnection.core.settingsGroupName
 * MMConnection.core.presetName
 */
class MMCoreConnector(
    private val core: CMMCore
) {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private val setup: microscenery.hardware.SPIMSetup

    val width: Int get() = core.imageWidth.toInt()
    val height: Int get() = core.imageHeight.toInt()
    val pixelSizeUm: Float get() = core.pixelSizeUm.toFloat()

    var snapTimes = listOf<Long>()
    var copyTimes = listOf<Long>()

    @Suppress("unused")
    val meanSnapTime
        get() = if (snapTimes.isNotEmpty()) snapTimes.sum() / snapTimes.size else 0

    @Suppress("unused")
    val meanCopyTime
        get() = if (copyTimes.isNotEmpty()) copyTimes.sum() / copyTimes.size else 0

    val stagePosition
        get() = Vector3f(
            setup.xStage.position.toFloat(),
            setup.yStage.position.toFloat(),
            setup.zStage.position.toFloat()
        )

    init {
        MicroscenerySettings.setIfUnset(Settings.Stage.Limits.OriginMoveProtection, true)

        logger.info(core.versionInfo)
        logger.info(core.apiVersionInfo)

        setup = microscenery.hardware.SPIMSetup.createDefaultSetup(core)

        updateSize()
    }

    fun updateSize() {
        if (width == 0) setup.snapImage()
    }

    @Suppress("unused")
    fun setRoi(roi: Rectangle) {
        core.setROI(roi.x, roi.y, roi.width, roi.height)
    }

    fun snapSlice(intoBuffer: ShortBuffer) {
        var snapTime = 0L
        var copyTime = 0L

        val start = System.currentTimeMillis()
        val img = setup.snapImage()
        snapTime += (System.currentTimeMillis() - start)

        val start2 = System.currentTimeMillis()
        intoBuffer.put(img.pix as ShortArray)
        (intoBuffer as Buffer).flip() // this cast has to be done to be compatible with JDK 8
        copyTime += (System.currentTimeMillis() - start2)
        recordTimes(snapTime, copyTime)
    }


    /**
     *  @param wait if true wait until stage reached target.
     */
    fun moveStage(target: Vector3f, wait: Boolean) {

        core.setXYPosition(target.x.toDouble(), target.y.toDouble())
        setup.zStage.position = target.z.toDouble()

        if (wait) {
            core.waitForDevice(core.xyStageDevice)
            core.waitForDevice(core.focusDevice)
        }
    }

    fun ablationShutter(open: Boolean, wait: Boolean){
        val shutterName = MicroscenerySettings.get(Settings.Ablation.MM.Shutter,core.shutterDevice)
        core.setShutterOpen(shutterName, open)

        if (wait)
            core.waitForDevice(shutterName)
    }

    fun laserPower(percent: Float){
        logger.debug("Would set laser power to $percent")
        //todo laserPower
    }

    private fun recordTimes(snap: Long, copy: Long) {
        snapTimes = snapTimes + (snap)
        while (snapTimes.size > 10)
            snapTimes = snapTimes.subList(1, snapTimes.size)
        copyTimes = copyTimes + (copy)
        while (copyTimes.size > 10)
            copyTimes = copyTimes.subList(1, copyTimes.size)
    }
}
