package microscenery.example.microscope

import fromScenery.utils.extensions.plus
import graphics.scenery.utils.lazyLogger
import microscenery.*
import microscenery.UI.StageSpaceUI
import microscenery.hardware.MicroscopeHardwareAgent
import microscenery.signals.*
import microscenery.stageSpace.MicroscopeLayout
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector2i
import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil
import java.awt.image.BufferedImage
import java.awt.image.DataBufferUShort
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ShortBuffer
import java.util.Random
import javax.imageio.ImageIO
import kotlin.concurrent.thread


class LocalFileScapeScene : DefaultScene(withSwingUI = false) {
    val stageSpaceManager: StageSpaceManager
    val msHub = MicrosceneryHub(hub)


    init {
        MicroscenerySettings.set(Settings.StageSpace.HideFocusFrame,false)
        MicroscenerySettings.set(Settings.StageSpace.HideFocusTargetFrame,true)


        val hardware = DataReaderMicroscope()

        stageSpaceManager = StageSpaceManager(
            hardware, scene, msHub, layout = MicroscopeLayout.Scape(MicroscopeLayout.Axis.Z, -0.5f),viewMode = true
        )

        lightSleepOnCondition { hardware.status().state == ServerState.MANUAL }



        thread {
            Thread.sleep(2000)
            hardware.readImages()
            while (true) {
                Thread.sleep(200)
                scene
                stageSpaceManager
            }
        }
    }

    override fun inputSetup() {
        super.inputSetup()
        StageSpaceUI(stageSpaceManager).stageUI(this,inputHandler, msHub)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            LocalFileScapeScene().main()
        }
    }
}


class DataReaderMicroscope :  MicroscopeHardwareAgent() {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private var idCounter = 1

    init {

        hardwareDimensions = HardwareDimensions(
            stageMin = Vector3f(-1000f),
            stageMax = Vector3f(1000f),
            imageSize = Vector2i(1024),
            vertexDiameter = 0.399f,
            numericType = NumericType.INT16
        )
        status = MicroscopeStatus(
            ServerState.MANUAL,
            stagePosition,
            false
        )

        //no need to start the agent
    }

    fun readImages(basePath: String = """D:\volumes\20240201_corvoria_samples"""){
        val baseDir = File(basePath)
        var min = Vector3f(Float.MAX_VALUE)
        var max = Vector3f(Float.MIN_VALUE)
        val rmd = Random()
        baseDir.listFiles()?.forEach { pictureFolder ->
            if (!pictureFolder.isDirectory) return@forEach
            val split = pictureFolder.name.split("-", limit = 2)
            val pos = Vector3f(split[0].toFloat(),split[1].toFloat(),0f)

            min = min.min(pos)
            max = max.max(pos)

            val randomVec = Vector3f(rmd.nextFloat()*0.1f)

            val imageFile = File(pictureFolder,"Default").walk().first { it.extension == "tif" }


            var img: BufferedImage? = null
            try {
                img = ImageIO.read(imageFile)
            } catch (_: IllegalArgumentException){
                logger.error("Could not find file ${imageFile.path}")
            } catch (e: IOException){
                logger.error(e.toString())
            }
            if (img == null) return

            val sb = ShortBuffer.wrap((img.raster.dataBuffer as DataBufferUShort).data)
            val bb = MemoryUtil.memAlloc(sb.capacity()*2)

            bb.asShortBuffer().put(sb);

            externalSnap(randomVec+pos, bb)
        }

        hardwareDimensions = hardwareDimensions.copy(stageMin = min, stageMax = max)
    }

    fun externalSnap(position: Vector3f, data: ByteBuffer){
        val sliceSignal = Slice(
            idCounter++,
            System.currentTimeMillis(),
            position,
            hardwareDimensions.byteSize,
            null,
            data
        )
        output.put(sliceSignal)
    }

    override fun moveStage(target: Vector3f) {
        TODO("Not yet implemented")
    }

    override fun onLoop() {
        TODO("Not yet implemented")
    }

    override fun goLive() {
        TODO("Not yet implemented")
    }

    override fun stop() {
        TODO("Not yet implemented")
    }

    override fun snapSlice() {
        TODO("Not yet implemented")
    }

    override fun shutdown() {
        TODO("Not yet implemented")
    }

    override fun acquireStack(meta: ClientSignal.AcquireStack) {
        TODO("Not yet implemented")
    }

    override fun ablatePoints(signal: ClientSignal.AblationPoints) {
        TODO("Not yet implemented")
    }

    override fun startAcquisition() {
        TODO("Not yet implemented")
    }


}

