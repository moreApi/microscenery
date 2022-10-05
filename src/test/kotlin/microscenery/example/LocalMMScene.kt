package microscenery.example

import microscenery.*
import microscenery.micromanagerConnection.MMConnection
import microscenery.micromanagerConnection.MicromanagerWrapper
import microscenery.network.ServerState
import org.joml.Vector3f

class LocalMMScene: DefaultScene() {

    init {


        val hardware: MicroscopeHardware = MicromanagerWrapper(MMConnection())
        val stageSpaceManager = StageSpaceManager(hardware,scene)

        lightSleepOnCondition { hardware.serverStatus().state == ServerState.MANUAL }

        stageSpaceManager.snapSlice(Vector3f(0f,0f,0f))
        stageSpaceManager.snapSlice(Vector3f(0f,0f,5f))
        stageSpaceManager.snapSlice(Vector3f(0f,0f,15f))
        stageSpaceManager.snapSlice(Vector3f(0f,0f,25f))
    }

    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            LocalMMScene().main()
        }
    }

}