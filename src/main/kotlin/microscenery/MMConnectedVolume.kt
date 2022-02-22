package microscenery

import graphics.scenery.Hub
import graphics.scenery.utils.LazyLogger
import graphics.scenery.utils.RingBuffer
import graphics.scenery.volumes.BufferedVolume
import graphics.scenery.volumes.Colormap
import graphics.scenery.volumes.TransferFunction
import graphics.scenery.volumes.Volume
import microscenery.hardware.SPIMSetup
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
class MMConnectedVolume(hub: Hub, private val slices:Int = 10, private val timeBetweenUpdates: Long = 0) {
    /** Logger for this application, will be instantiated upon first use. */
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))
    val mmConnection = MMConnection(slices)
    val volume: BufferedVolume
    var running = true

    init {


        volume = Volume.fromBuffer(emptyList(), mmConnection.width, mmConnection.height, slices, UnsignedShortType(), hub)

        volume.name = "volume"
        volume.spatial {
            position = Vector3f(0.0f, 0.0f, 0.0f)
            scale = Vector3f(0.1f, 0.1f, 0.7f)
        }
        volume.colormap = Colormap.get("hot")
        volume.pixelToWorldRatio = 0.1f

        volume.transferFunction = TransferFunction.ramp(0.001f,distance = 1f)
        /*with(volume.transferFunction) {
            addControlPoint(0.0f, 0.0f)
            addControlPoint(7000.0f, 1.0f)
        }*/

        volume.metadata["animating"] = true

        thread {
            var count = 0
            val volumeBuffer =
                RingBuffer<ByteBuffer>(2,default =  {
                    MemoryUtil.memAlloc((mmConnection.width * mmConnection.height * slices * Short.SIZE_BYTES)) })

            //var secondTimer = System.currentTimeMillis()
            //var lastCount = 0
            //var deltaTime = System.currentTimeMillis()
            //var deltas = emptyList<Int>()
            while (running) {
                if (volume.metadata["animating"] == true) {
                    val currentBuffer = volumeBuffer.get()
                    val start = System.currentTimeMillis()
                    mmConnection.captureStack(currentBuffer.asShortBuffer())
                    println("capture stack took ${System.currentTimeMillis()-start}ms")

                    //move z Stage to see change
                    //setup.zStage.position = Math.exp(count.toDouble())

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
}