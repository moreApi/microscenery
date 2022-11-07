package microscenery.example.network

import microscenery.hardware.DemoMicroscopeHardware
import microscenery.network.RemoteMicroscopeServer
import microscenery.network.SliceStorage
import org.zeromq.ZContext


class RemoteMicroscopeServerDemoExample {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // settings like port can be set in [microscenery.properties]
            val zContext = ZContext()

            val microscope = DemoMicroscopeHardware()

            @Suppress("UNUSED_VARIABLE")
            val server =
                RemoteMicroscopeServer(microscope, storage = SliceStorage(500 * 1024 * 1024), zContext = zContext)
        }
    }
}
