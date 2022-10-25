package microscenery.example.network

import microscenery.hardware.MicroscopeHardware
import microscenery.hardware.micromanagerConnection.MMConnection
import microscenery.hardware.micromanagerConnection.MicromanagerWrapper
import microscenery.network.RemoteMicroscopeServer
import microscenery.network.SliceStorage
import org.zeromq.ZContext


class RemoteMicroscopeServerMMExample {
    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            // settings like port can be set in [microscenery.properties]
            val zContext = ZContext()

            val hardware: MicroscopeHardware = MicromanagerWrapper(MMConnection())
            val imgSize = hardware.hardwareDimensions().imageSize
            @Suppress("UNUSED_VARIABLE")
            val server = RemoteMicroscopeServer(hardware, storage = SliceStorage(500*imgSize.x*imgSize.y), zContext = zContext)
        }
    }
}
