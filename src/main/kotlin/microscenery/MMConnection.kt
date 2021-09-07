package microscenery

import graphics.scenery.*
import graphics.scenery.backends.Renderer
import graphics.scenery.utils.RingBuffer
import graphics.scenery.volumes.BufferedVolume
import graphics.scenery.volumes.Colormap
import graphics.scenery.volumes.Volume
import mmcorej.CMMCore
import net.imglib2.type.numeric.integer.UnsignedShortType
import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.ShortBuffer
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

class MMConnection : SceneryBase(MMConnection::class.java.simpleName, windowWidth = 1920, windowHeight = 1200) {

    private val core = CMMCore()
    lateinit var volume: BufferedVolume

    val slices = 30

    override fun init() {
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


        val info = core.versionInfo
        println(info)
        core.loadSystemConfiguration("C:/Program Files/Micro-Manager-2.0gamma/MMConfig_demo2.cfg")
        core.setConfig("screen", "1024")
        core.snapImage()
        //val img1 = core.image as ShortArray// returned as a 1D array of signed integers in row-major order
        val width = core.imageWidth
        val height = core.imageHeight
        volume = createVolume(width.toInt(), height.toInt())
        scene.addChild(volume)

        thread {
            var count = 0
            val volumeBuffer =
                RingBuffer<ByteBuffer>(2) { MemoryUtil.memAlloc((width * height * slices * Short.SIZE_BYTES).toInt()) }

            var secondTimer = System.currentTimeMillis()
            var lastCount = 0
            var deltaTime = System.currentTimeMillis()
            var deltas = emptyList<Int>()
            while (running) {
                if (volume.metadata["animating"] == true) {
                    val currentBuffer = volumeBuffer.get()
                    captureStack(currentBuffer.asShortBuffer())

                    volume.lock.withLock {
                        volume.addTimepoint("t-${count}", currentBuffer)
                        volume.goToLastTimepoint()
                        volume.purgeFirst(3, 3)
                    }

                    count++

                    deltas = deltas + (System.currentTimeMillis() - deltaTime).toInt()
                    deltaTime = System.currentTimeMillis()

                    if (System.currentTimeMillis() - secondTimer > 1000) {
                        logger.warn("sps: ${count - lastCount} mean delta: ${deltas.average()}")
                        lastCount = count
                        secondTimer = System.currentTimeMillis()
                    }
                }

                //Thread.sleep(333L)
            }
        }

    }

    private fun createVolume(width: Int, height: Int): BufferedVolume {
        val volume = Volume.fromBuffer(emptyList(), width, height, slices, UnsignedShortType(), hub)

        volume.name = "volume"
        volume.spatial {
            position = Vector3f(0.0f, 0.0f, 0.0f)
            scale = Vector3f(0.1f, 0.1f, 10f)
        }
        volume.colormap = Colormap.get("hot")
        volume.pixelToWorldRatio = 0.03f

        with(volume.transferFunction) {
            addControlPoint(0.0f, 0.0f)
            addControlPoint(7000.0f, 1.0f)
        }

        volume.metadata["animating"] = true

        return volume
    }

    private fun captureStack(intoBuffer: ShortBuffer) {
        var offset = 0
        (0 until slices).forEach { _ ->
            core.snapImage()
            val sa = core.image as ShortArray
            sa.forEach {
                intoBuffer.put(offset, it)
                offset += 1
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            MMConnection().main()
        }
    }
}