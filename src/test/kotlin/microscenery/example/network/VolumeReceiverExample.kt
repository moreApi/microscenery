package microscenery.example.network

import microscenery.network.VolumeReceiver
import org.zeromq.ZContext


class VolumeReceiverExample {
    val volumeSize = 1000*100*100

    val receiver = VolumeReceiver(true, zContext = ZContext(), volumeSize)

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

