package microscenery.example.network

import graphics.scenery.utils.RingBuffer
import microscenery.MMConnection
import microscenery.network.VolumeSender
import org.lwjgl.system.MemoryUtil
import org.zeromq.ZContext
import java.awt.Rectangle
import java.nio.ByteBuffer
import kotlin.concurrent.thread

const val connections: Int = 1
const val basePort: Int = 4400

val zContext = ZContext()

class MMVolumeSenderScene{

    val mmConnection = MMConnection()

    val volumeSize = mmConnection.width * mmConnection.height * mmConnection.slices * Short.SIZE_BYTES
    val volumeSender = VolumeSender(connections, basePort, zContext)

    val timeBetweenUpdates = 1000

    val volumeBuffers = RingBuffer<ByteBuffer>(2, default = {
        MemoryUtil.memAlloc((mmConnection.width * mmConnection.height * mmConnection.slices * Short.SIZE_BYTES))
    })
    var time = 0L

    var running = true


    init {
        println("Start MM Sender with  ${mmConnection.width}x${mmConnection.height}x${mmConnection.slices}xShort = $volumeSize bytes at port $basePort")

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
                val input = readLine()?.trim() ?: continue
                println("got $input")

                when {
                    input == "q" -> {
                        running = false
                    }
                    input.startsWith("roi") -> {
                        val v = input.substringAfter("roi").trim().split(",").map { it.toInt() }.toList()
                        val r = Rectangle(v[0],v[1],v[2],v[3])
                        println("Setting ROI to $r")
                        mmConnection.setRoi(r)
                    }
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