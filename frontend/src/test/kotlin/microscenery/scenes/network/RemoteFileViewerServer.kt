package microscenery.scenes.network

import microscenery.DemoMicroscopeHardware
import microscenery.FileMicroscopeHardware
import microscenery.network.BonjourService
import microscenery.network.RemoteMicroscopeServer
import microscenery.network.SliceStorage
import org.zeromq.ZContext
import kotlin.concurrent.thread

class RemoteFileViewerServer {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // settings like port can be set in [microscenery.properties]
            val zContext = ZContext()

            val microscope = FileMicroscopeHardware("""D:\volumes\spindle\NikonSD_100x_R1EmESC_01-1.tif""")

            @Suppress("UNUSED_VARIABLE")
            val server =
                RemoteMicroscopeServer(microscope, storage = SliceStorage(500 * 1024 * 1024), zContext = zContext, acquireOnConnect = true)

            val bonjour = BonjourService()
            bonjour.register("FileViewerServer",server.basePort, microscope.hardwareDimensions().imageMeta.toString())

            thread {
                while (true){
                    val inn = readln().strip()
                    println("got $inn")
                    if ( inn  == "q"){
                        println("closing")
                        bonjour.close()
                        server.shutdown()
                        break
                    }
                }
            }
        }
    }
}