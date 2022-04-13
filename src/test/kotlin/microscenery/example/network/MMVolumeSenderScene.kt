package microscenery.example.network

import graphics.scenery.utils.RingBuffer
import microscenery.MMConnection
import microscenery.network.VolumeSender
import org.lwjgl.system.MemoryUtil
import org.zeromq.ZContext
import java.nio.ByteBuffer
import kotlin.concurrent.thread

const val slices: Int = 112
const val connections: Int = 1
const val basePort: Int = 4400

val zContext = ZContext()

class MMVolumeSenderScene {

    val mmConnection = MMConnection(slices)

    val volumeSize = mmConnection.width * mmConnection.height * slices * Short.SIZE_BYTES
    val volumeSender = VolumeSender(connections, basePort, zContext)

    val timeBetweenUpdates = 1000

    val volumeBuffers = RingBuffer<ByteBuffer>(2, default = {
        MemoryUtil.memAlloc((mmConnection.width * mmConnection.height * slices * Short.SIZE_BYTES))
    })
    var time = 0L


    init {
        println("Start MM Sender with  ${mmConnection.width}x${mmConnection.height}x${slices}xShort = $volumeSize bytes at port $basePort")
        thread {
            while (true) {
                //wait at least timeBetweenUpdates
                (System.currentTimeMillis() - time).let {
                    if (it in 1..timeBetweenUpdates)
                        Thread.sleep(timeBetweenUpdates - it)
                }
                time = System.currentTimeMillis()

                val buf = volumeBuffers.get()
                buf.clear()
                mmConnection.captureStack(buf.asShortBuffer())
                buf.rewind()
                volumeSender.sendVolume(buf)
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            MMVolumeSenderScene()
        }
    }
}