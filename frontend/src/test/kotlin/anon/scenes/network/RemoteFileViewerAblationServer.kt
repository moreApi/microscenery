package anon.scenes.network

import fromScenery.SettingsEditor
import graphics.scenery.Origin
import anon.FileMicroscopeHardware
import anon.MicroscenerySettings
import anon.network.RemoteMicroscopeServer
import anon.network.SliceStorage
import anon.signals.BaseServerSignal
import anon.signals.ServerType
import anon.simulation.AblationSimulationMicroscope
import org.zeromq.ZContext

/**
 * Simulated ablation on files
 */
class RemoteFileViewerAblationServer {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // settings like port can be set in [anon.properties]
            val zContext = ZContext()

            //val microscope = FileMicroscopeHardware("""D:\volumes\spindle\NikonSD_100x_R1EmESC_01-1.tif""")
            val microscope = FileMicroscopeHardware("""volumes/Lund-100MB.tif""")

            RemoteMicroscopeServer(
                AblationSimulationMicroscope(microscope, imgOrigin = Origin.FrontBottomLeft),
                storage = SliceStorage(500 * 1024 * 1024),
                zContext = zContext,
                announceWithBonjour = true,
                acquireOnConnect = true,
                serverHello = BaseServerSignal.ServerHello(
                    "File Microscope",
                    ServerType.MICROSCOPE,
                    "Ablation Simulation"
                ))

            SettingsEditor(MicroscenerySettings)

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