package microscenery.example.network

import microscenery.network.VolumeReceiver
import org.zeromq.ZContext


class VolumeReceiverExample {
    val basePort = 4000
    val connections = 10
    val volumeSize = 1000*100*100
    val host = "10.1.39.84"

    val receiver = VolumeReceiver(volumeSize, connections, basePort,true,host,zContext = ZContext())

    fun run(){
        while (true){
            val result = receiver.getVolume(5000)
            if (result == null){
                println("got nuthin, listning hardr now")
                continue
            }
            println("Got a beautiful Volume that starts with ${result.get()}, ${result.get()}, " +
                    "${result.get()}, ${result.get()}, ${result.get()}, ${result.get()}")

        }
    }

    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            VolumeReceiverExample().run()
        }
    }

}

