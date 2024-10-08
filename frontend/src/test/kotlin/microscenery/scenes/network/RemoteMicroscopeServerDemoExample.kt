package microscenery.scenes.network

import microscenery.DemoMicroscopeHardware
import microscenery.scenes.microscope.FolderReaderMicroscope
import microscenery.network.RemoteMicroscopeServer
import microscenery.network.SliceStorage
import org.zeromq.ZContext
import kotlin.concurrent.thread


class RemoteMicroscopeServerDemoExample {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // settings like port can be set in [microscenery.properties]
            val zContext = ZContext()

            val microscope = DemoMicroscopeHardware()
            //val microscope = FolderReaderMicroscope()

            @Suppress("UNUSED_VARIABLE")
            val server =
                RemoteMicroscopeServer(microscope, storage = SliceStorage(500 * 1024 * 1024), zContext = zContext)

            thread {
                Thread.sleep(10000)
                //microscope.readImages()
            }
        }
    }
}
