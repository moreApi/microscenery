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

class MMVolumeSenderScene{

    val mmConnection = MMConnection(slices)

    val volumeSize = mmConnection.width * mmConnection.height * slices * Short.SIZE_BYTES
    val volumeSender = VolumeSender(connections, basePort, zContext)

    val timeBetweenUpdates = 1000

    val volumeBuffers = RingBuffer<ByteBuffer>(2, default = {
        MemoryUtil.memAlloc((mmConnection.width * mmConnection.height * slices * Short.SIZE_BYTES))
    })
    var time = 0L

    var running = true


    init {
        println("Start MM Sender with  ${mmConnection.width}x${mmConnection.height}x${slices}xShort = $volumeSize bytes at port $basePort")

        // imaging and sending thread
        thread {
            Thread.sleep(200)
            while (running) {
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
            println("stopped capturing")
            zContext.destroy()

        }

        //io thread
        thread {
            while (running){
                val input = readLine()
                println("got "+input)

                if (input?.trim() == "q"){
                    running = false
                }
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