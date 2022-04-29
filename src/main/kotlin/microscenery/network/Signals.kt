package microscenery.network

import org.joml.Vector3i

sealed class ClientSignal(){
    class StartImaging():ClientSignal()
    class StopImaging():ClientSignal()
    class ClientSignOn():ClientSignal()
    class Shutdown():ClientSignal()
}

sealed class ServerSignal() {
    data class Status(val imageSize: Vector3i = Vector3i(0),
                      val state: ServerState = ServerState.Paused,
                      val dataPorts: List<Int> = emptyList()) : ServerSignal()
}

enum class ServerState{
    Imaging,Paused,ShuttingDown
}