package microscenery.network

import org.joml.Vector3f

sealed class ClientSignal(){
    class StartImaging():ClientSignal()
    class StopImaging():ClientSignal()
    class ClientSignOn():ClientSignal()
}

sealed class ServerSignal() {
    data class Status(val imageSize: Vector3f, val status: ServerStatus, val dataPorts: List<Int>) : ServerSignal()
}

enum class ServerStatus{
    Started,Imaging,Paused,ShuttingDown
}