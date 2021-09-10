package microscenery

import graphics.scenery.Hub
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

/**
 *
 * @param slices Should be not divisible by 32, otherwise the animation will be a standing wave.
 */
class MMConnectedVolume(hub: Hub, private val slices:Int = 10, private val timeBetweenUpdates: Long = 333) {

    private val core = CMMCore()
    val volume: BufferedVolume
    var running = true

    init {
        val info = core.versionInfo
        println(info)
        core.loadSystemConfiguration("C:/Program Files/Micro-Manager-2.0gamma/MMConfig_demo2.cfg")
        core.setConfig("screen", "1024")
        core.snapImage()
        val width = core.imageWidth.toInt()
        val height = core.imageHeight.toInt()

        volume = Volume.fromBuffer(emptyList(), width, height, slices, UnsignedShortType(), hub)

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

        thread {
            var count = 0
            val volumeBuffer =
                RingBuffer<ByteBuffer>(2) { MemoryUtil.memAlloc((width * height * slices * Short.SIZE_BYTES)) }

            //var secondTimer = System.currentTimeMillis()
            //var lastCount = 0
            //var deltaTime = System.currentTimeMillis()
            //var deltas = emptyList<Int>()
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
/*
                    deltas = deltas + (System.currentTimeMillis() - deltaTime).toInt()
                    deltaTime = System.currentTimeMillis()

                    if (System.currentTimeMillis() - secondTimer > 1000) {
                        logger.warn("sps: ${count - lastCount} mean delta: ${deltas.average()}")
                        lastCount = count
                        secondTimer = System.currentTimeMillis()
                    }
  */
                }

                if (timeBetweenUpdates > 0) { Thread.sleep(timeBetweenUpdates) }
            }
        }
    }

    private fun captureStack(intoBuffer: ShortBuffer) {
        var offset = 0
        (0 until slices).forEach { _ ->
            core.snapImage()
            //val img1 = core.image as ShortArray// returned as a 1D array of signed integers in row-major order
            val sa = core.image as ShortArray
            sa.forEach {
                intoBuffer.put(offset, it)
                offset += 1
            }
        }
    }
}