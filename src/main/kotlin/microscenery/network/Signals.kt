package microscenery.network

import org.joml.Vector3i

sealed class ClientSignal {
    object StartImaging : ClientSignal()
    object StopImaging : ClientSignal()
    object ClientSignOn : ClientSignal()
    object Shutdown : ClientSignal()
    object SnapStack: ClientSignal()
}

sealed class ServerSignal {
    data class Status(
        val imageSize: Vector3i = Vector3i(0),
        val state: ServerState = ServerState.Paused,
        val dataPorts: List<Int> = emptyList(),
        val connectedClients: Int = 0
    ) : ServerSignal()
    object StackAcquired: ServerSignal()
}

enum class ServerState {
    Imaging, Paused, ShuttingDown, Snapping
}