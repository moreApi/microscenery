package microscenery.example

import graphics.scenery.*
import graphics.scenery.backends.Renderer
import graphics.scenery.utils.RingBuffer
import graphics.scenery.utils.VideoEncoder
import microscenery.MMConnection
import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import kotlin.concurrent.thread

class VolumeStreamer : SceneryBase("streamer", wantREPL = false) {

    val encoder: VideoEncoder
    val mmConnection = MMConnection(112)
    val volumeBuffer: RingBuffer<ByteBuffer>
    val frameBuffer: ByteBuffer
    var currentDataBuffer: ByteBuffer? = null

    val fps = 2

    init {
//        val hub = Hub()
//        val settings = Settings(hub)
//        hub.add(settings)
        settings.set("VideoEncoder.Format", "HEVC")

//             encoder = VideoEncoder(mmConnection.width, mmConnection.height, "fromMM",hub=hub,fps=1)
        encoder =
            VideoEncoder(mmConnection.width, mmConnection.height, "fromMM", hub = hub, fps = fps, networked = false)

        volumeBuffer = RingBuffer<ByteBuffer>(3, default = {
            MemoryUtil.memAlloc(mmConnection.width * mmConnection.height * 112 * Short.SIZE_BYTES)
        })
        frameBuffer = MemoryUtil.memAlloc(mmConnection.width * mmConnection.height * 4 * Byte.SIZE_BYTES)

    }

    override fun init() {
        super.init()

        // mm image capturing is faster if a scenerz scene is open, so here is our dummy scene
        renderer = hub.add(
            SceneryElement.Renderer,
            Renderer.createRenderer(hub, applicationName, scene, 512, 512)
        )

        val light = PointLight(radius = 15.0f)
        light.spatial().position = Vector3f(0.0f, 0.0f, 2.0f)
        light.intensity = 5.0f
        light.emissionColor = Vector3f(1.0f, 1.0f, 1.0f)
        scene.addChild(light)

        val cam: Camera = DetachedHeadCamera()
        with(cam) {
            spatial {
                position = Vector3f(0.0f, 0.0f, 15.0f)
            }
            perspectiveCamera(50.0f, 512, 512)

            scene.addChild(this)
        }

//        mmVol = MMConnectedVolume(hub, slices)
//        scene.addChild(mmVol.volume)


        stream()
    }

    fun stream() {


        thread {
            while (running) {
                val dataBuffer = volumeBuffer.get()
                dataBuffer.rewind()
                val start = System.currentTimeMillis()
                mmConnection.captureStack(dataBuffer.asShortBuffer())
                currentDataBuffer = dataBuffer
                println("capture stack took ${System.currentTimeMillis() - start}ms")
            }
        }
        thread {
            while (running) {
                for (f in 0 until fps) {
                    currentDataBuffer?.let {
                        frameBuffer.rewind()
                        writeToFrameBuffer(frameBuffer, it, 50)
                        encoder.encodeFrame(frameBuffer)
                    }
                    Thread.sleep(1000L / fps)
                }
            }

            encoder.finish()
        }

        thread {
            val recordSec = 60
            while (currentDataBuffer == null)
                Thread.sleep(50)
            var counter = 0
            while (counter++ <= recordSec) {
                Thread.sleep(1000)
                println("Time: $counter sec")

            }
            close()
        }
    }


    private fun writeToFrameBuffer(
        frameBuffer: ByteBuffer,
        dataBuffer: ByteBuffer,
        slice: Int
    ): ByteBuffer {
        for (i in 0 until mmConnection.width * mmConnection.height) {
            dataBuffer.position(mmConnection.width * mmConnection.height * (slice) * Short.SIZE_BYTES + i * Short.SIZE_BYTES)
            val value = dataBuffer.short.toByte()
            frameBuffer.put(value)
            dataBuffer.position(mmConnection.width * mmConnection.height * (slice+1) * Short.SIZE_BYTES + i * Short.SIZE_BYTES)
            val value2 = dataBuffer.short.toByte()
            frameBuffer.put(value2)
            dataBuffer.position(mmConnection.width * mmConnection.height * (slice+2) * Short.SIZE_BYTES + i * Short.SIZE_BYTES)
            val value3 = dataBuffer.short.toByte()
            frameBuffer.put(value3)

            frameBuffer.put(Byte.MAX_VALUE)
        }
        frameBuffer.rewind()
        return frameBuffer
    }


    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val vs = VolumeStreamer()
            vs.main()
        }
    }
}