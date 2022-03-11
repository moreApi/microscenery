package microscenery.example.network

import microscenery.network.VolumeSender
import org.lwjgl.system.MemoryUtil

class VolumeSenderExample {

    val basePort = 4000
    val connections = 10
    val host = "10.1.39.84"
    val volumeSize = 1000 * 100 * 100
    val repeats = 5


    val sender = VolumeSender(connections, basePort,host)

    fun run() {

        val data = MemoryUtil.memAlloc(volumeSize)
        for (x in 1 .. 100)
            data.put(x.toByte())
        data.rewind()

        for (x in 1 .. repeats){
            println("Sending Volume $x")
            sender.sendVolume(data)
            data.rewind()
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            VolumeSenderExample().run()
        }
    }
}

