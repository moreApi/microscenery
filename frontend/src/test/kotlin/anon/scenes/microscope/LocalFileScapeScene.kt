package anon.scenes.microscope

import graphics.scenery.utils.lazyLogger
import anon.DefaultScene
import anon.MicrosceneryHub
import anon.UI.ScapeViewerUI
import anon.UI.StageSpaceUI
import anon.hardware.MicroscopeHardwareAgent
import anon.lightSleepOnCondition
import anon.signals.*
import anon.stageSpace.MicroscopeLayout
import anon.stageSpace.StageSpaceManager
import org.joml.Vector2i
import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil
import java.awt.image.BufferedImage
import java.awt.image.DataBufferUShort
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ShortBuffer
import java.util.concurrent.Semaphore
import javax.imageio.ImageIO
import kotlin.concurrent.thread


class LocalFileScapeScene : DefaultScene(withSwingUI = true) {
    val stageSpaceManager: StageSpaceManager
    val msHub = MicrosceneryHub(hub)


    init {
        ScapeViewerUI.scapeViewerSettings()


        val hardware = FolderReaderMicroscope()

        stageSpaceManager = StageSpaceManager(
            hardware, scene, msHub, layout = MicroscopeLayout.Scape(MicroscopeLayout.Axis.Z, 40.5), viewMode = true
        )

        lightSleepOnCondition { hardware.status().state == ServerState.MANUAL }


        thread {
            Thread.sleep(5000)
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
        val scapeUI = ScapeViewerUI(msHub)
        scapeUI.resetView()
        StageSpaceUI(stageSpaceManager).stageUI(this, inputHandler, msHub, scapeUI.orthoViewCommands())
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            LocalFileScapeScene().main()
        }
    }
}


class FolderReaderMicroscope : MicroscopeHardwareAgent() {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private var idCounter = 1

    init {

        val im = ImageMeta(
            imageSize = Vector2i(1024),
            vertexDiameter = 0.399f,
            numericType = NumericType.INT16
        )

        hardwareDimensions = HardwareDimensions(
            stageMin = Vector3f(-1000f),
            stageMax = Vector3f(1000f),
            im
        )
        imageMeta = im
        status = MicroscopeStatus(
            ServerState.MANUAL,
            stagePosition,
            false
        )

        //no need to start the agent
    }

    fun readImages(basePath: String = """D:\volumes\20240201_corvoria_samples""") {
        val baseDir = File(basePath)
        var min = Vector3f(Float.MAX_VALUE)
        var max = Vector3f(Float.MIN_VALUE)
        baseDir.listFiles()?.forEach { pictureFolder ->
            if (!pictureFolder.isDirectory) return@forEach
            val split = pictureFolder.name.split("-", limit = 2)
            val pos = Vector3f(split[0].toFloat(), split[1].toFloat(), 0f)

            min = min.min(pos)
            max = max.max(pos)

            val imageFile = File(pictureFolder, "Default").walk().first { it.extension == "tif" }


            var img: BufferedImage? = null
            try {
                img = ImageIO.read(imageFile)
            } catch (_: IllegalArgumentException) {
                logger.error("Could not find file ${imageFile.path}")
            } catch (e: IOException) {
                logger.error(e.toString())
            }
            if (img == null) return

            val sb = ShortBuffer.wrap((img.raster.dataBuffer as DataBufferUShort).data)
            val bb = MemoryUtil.memAlloc(sb.capacity() * 2)

            bb.asShortBuffer().put(sb)

            externalSnap(pos, bb)
        }

        hardwareDimensions = hardwareDimensions.copy(stageMin = min, stageMax = max)
    }

    fun externalSnap(position: Vector3f, data: ByteBuffer) {
        val sliceSignal = Slice(
            idCounter++,
            System.currentTimeMillis(),
            position,
            hardwareDimensions.byteSize,
            null,
            imageMeta,
            data
        )
        output.put(MicroscopeSlice(sliceSignal))
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

    override fun sync(): Semaphore {
        return Semaphore(1) // It's already free. Come get it!
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

    override fun acquireStack(meta: MicroscopeControlSignal.AcquireStack) {
        TODO("Not yet implemented")
    }

    override fun ablatePoints(signal: MicroscopeControlSignal.AblationPoints) {
        TODO("Not yet implemented")
    }

    override fun startAcquisition() {
        TODO("Not yet implemented")
    }


}

