package microscenery.hardware.micromanagerConnection

import graphics.scenery.Camera
import graphics.scenery.utils.LazyLogger
import graphics.scenery.utils.extensions.xy
import graphics.scenery.volumes.Colormap
import graphics.scenery.volumes.TransferFunction
import microscenery.DefaultScene
import microscenery.MicroscenerySettings
import microscenery.StreamedVolume
import microscenery.hardware.SPIMSetup
import microscenery.let
import mmcorej.CMMCore
import org.joml.Vector3f
import java.awt.Rectangle
import java.nio.ShortBuffer
import kotlin.concurrent.thread

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
class MMConnection @JvmOverloads constructor(
    core_: CMMCore? = null
) {
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private val core: CMMCore
    private val setup: SPIMSetup

    var width: Int = 0
    var height: Int = 0

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

        setup = SPIMSetup.createDefaultSetup(core)

        updateSize()
    }

    fun updateSize() {
        setup.snapImage() // do this so the following parameters are set
        width = core.imageWidth.toInt()
        height = core.imageHeight.toInt()
    }

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
        if (!stagePosition.xy().equals(target.xy(), MicroscenerySettings.get("Stage.precisionXY", 1.0f)))
            core.setXYPosition(target.x.toDouble(), target.y.toDouble())

        val precision = MicroscenerySettings.get("Stage.precisionZ", 1.0f)
        val from = stagePosition.z
        val to = target.z

        if (to < from - precision || from + precision < to) {
            setup.zStage.position = to.toDouble()
        }

        // device name xyStage = "XY" ??
        if (wait) {
            core.waitForDevice("XY")
        }

    }

    private fun recordTimes(snap: Long, copy: Long) {
        snapTimes = snapTimes + (snap)
        while (snapTimes.size > 10)
            snapTimes = snapTimes.subList(1, snapTimes.size)
        copyTimes = copyTimes + (copy)
        while (copyTimes.size > 10)
            copyTimes = copyTimes.subList(1, copyTimes.size)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            DefaultScene({ scene, hub ->

                val mmConnection = MMConnection()
                mmConnection.moveStage(Vector3f(10f), false)

                val mmVol = StreamedVolume(
                    hub,
                    mmConnection.width,
                    mmConnection.height,
                    10
                ) {
                    //mmConnection.captureStack(it.asShortBuffer())
                    it
                }
                scene.addChild(mmVol.volume)
                mmVol.volume.spatial().scale = Vector3f(0.1f, 0.1f, 0.4f)
                mmVol.volume.colormap = Colormap.get("plasma")
                mmVol.volume.transferFunction = TransferFunction.ramp()
                mmVol.volume
                    .converterSetups.first()
                    .setDisplayRange(17.0, 3000.0)

                (scene.findByClassname("Camera").first() as Camera).spatial().position = Vector3f(2f, -5f, 7f)

                thread {
                    while (true) {
                        Thread.sleep(200)
                        @Suppress("UNUSED_EXPRESSION")
                        scene
                    }
                }

            }).main()
        }
    }
}
