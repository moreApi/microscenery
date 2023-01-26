package microscenery.hardware.micromanagerConnection


import fromScenery.LazyLogger
import microscenery.MicroscenerySettings
import microscenery.let
import mmcorej.CMMCore
import org.joml.Vector3f
import java.awt.Rectangle
import java.nio.ShortBuffer

/**
 * Connection to MicroManger Core. Does the imaging.
 *
 * RelevantProperties:
 * MMConnection.core.configuration
 * MMConnection.core.settingsGroupName
 * MMConnection.core.presetName
 */
class MMConnection @JvmOverloads constructor(
    core_: CMMCore? = null
) {
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private val core: CMMCore
    private val setup: microscenery.hardware.SPIMSetup

    var width: Int = 0
    var height: Int = 0
    var pixelSizeUm: Float = 0f

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
        MicroscenerySettings.setIfUnset("MMConnection.OriginMoveProtection", true)

        if (core_ != null) {
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

        setup = microscenery.hardware.SPIMSetup.createDefaultSetup(core)

        updateSize()
    }

    fun updateSize() {
        setup.snapImage() // do this so the following parameters are set
        width = core.imageWidth.toInt()
        height = core.imageHeight.toInt()
        pixelSizeUm = core.pixelSizeUm.toFloat()
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
        intoBuffer.flip()
        copyTime += (System.currentTimeMillis() - start2)
        recordTimes(snapTime, copyTime)
    }


    /**
     *  @param wait if true wait until stage reached target.
     */
    fun moveStage(target: Vector3f, wait: Boolean) {
        if (MicroscenerySettings.get("MMConnection.OriginMoveProtection", true)
            && target == Vector3f(0f)){//( target.x == 0f || target.y == 0f || target.z == 0f)){
            logger.warn("Ignoring stage move command because MMConnection.OriginMoveProtection is true")
            return
        }

        core.setXYPosition(target.x.toDouble(), target.y.toDouble())
        setup.zStage.position = target.z.toDouble()

        if (wait) {
            core.waitForDevice(core.xyStageDevice)
            core.waitForDevice(core.focusDevice)
        }

    }

    fun ablationShutter(open: Boolean, wait: Boolean){
        val shutterName = MicroscenerySettings.get("Ablation.Shutter",core.shutterDevice)
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
