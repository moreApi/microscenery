package microscenery.example.network

import microscenery.network.ControlledVolumeStreamServer

class ControlledVolumeStreamServerExample {
    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            // settings like port can be set in [microscenery.properties]
            ControlledVolumeStreamServer()
        }
    }
}
