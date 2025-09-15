package anon.scenes.network

import anon.hardware.MicroscopeHardware
import anon.hardware.micromanagerConnection.MMCoreConnector
import anon.hardware.micromanagerConnection.MicromanagerWrapper
import anon.network.RemoteMicroscopeServer
import anon.network.SliceStorage
import anon.scenes.microscope.LocalMMScene.Companion.initLocalMMCoreFake
import org.zeromq.ZContext


class RemoteMicroscopeServerMMExample {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // settings like port can be set in [anon.properties]
            val zContext = ZContext()

            val hardware: MicroscopeHardware = MicromanagerWrapper(MMCoreConnector(initLocalMMCoreFake()))
            val imgSize = hardware.hardwareDimensions().imageSize

            @Suppress("UNUSED_VARIABLE")
            val server = RemoteMicroscopeServer(
                hardware,
                storage = SliceStorage(500 * imgSize.x * imgSize.y),
                zContext = zContext
            )
        }
    }
}
