package microscenery.scenes.network

import graphics.scenery.Origin
import microscenery.FileMicroscopeHardware
import microscenery.network.BonjourService
import microscenery.network.RemoteMicroscopeServer
import microscenery.network.SliceStorage
import microscenery.simulation.AblationSimulationMicroscope
import org.zeromq.ZContext

class RemoteFileViewerServer {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // settings like port can be set in [microscenery.properties]
            val zContext = ZContext()

            //val microscope = FileMicroscopeHardware("""D:\volumes\spindle\NikonSD_100x_R1EmESC_01-1.tif""")
            val microscope = FileMicroscopeHardware("""volumes/Lund-100MB.tif""")

            val server =
                RemoteMicroscopeServer(
                    AblationSimulationMicroscope(microscope, imgOrigin = Origin.FrontBottomLeft),
                    storage = SliceStorage(500 * 1024 * 1024),
                    zContext = zContext,
                    announceWithBonjour = true,
                    acquireOnConnect = true)

            val bonjour = BonjourService()
            bonjour.register("Microscope_FileViewerServer",server.basePort, microscope.hardwareDimensions().imageMeta.toString())

//            thread {
//                while (true){
//                    val inn = readln().trim()
//                    println("got $inn")
//                    if ( inn  == "q"){
//                        println("closing")
//                        bonjour.close()
//                        server.shutdown()
//                        break
//                    }
//                }
//            }
        }
    }
}