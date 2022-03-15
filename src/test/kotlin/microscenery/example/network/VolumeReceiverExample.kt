package microscenery.example.network

import microscenery.network.VolumeReceiver
import org.zeromq.ZContext


class VolumeReceiverExample {
    val basePort = 4000
    val connections = 1
    val volumeSize = 1000*100*100
    val host = "10.1.39.84"

    val receiver = VolumeReceiver(volumeSize, connections, basePort,true,host,zContext = ZContext())

    var volumes = 0

    fun run(){
        val start = System.currentTimeMillis()
        while (true){
            val result = receiver.getVolume(5000)
            if (result == null){
                println("got nothing, listning hardr now")
                continue
            }
            val delta = System.currentTimeMillis() - start
            println("Got a beautiful Volume that starts with ${result.get()}, ${result.get()}, " +
                    "${result.get()}, ${result.get()}, ${result.get()}, ${result.get()}")
            println("Volumes ${++volumes}  ${((volumes.toLong() * volumeSize) / delta )/ 1000}mB/s")

        }
    }

    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            VolumeReceiverExample().run()
        }
    }

}

