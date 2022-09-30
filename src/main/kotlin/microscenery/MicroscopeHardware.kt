package microscenery

import microscenery.network.ServerSignal
import org.joml.Vector3f
import java.nio.ByteBuffer
import java.util.concurrent.BlockingQueue

interface MicroscopeHardware{
    fun moveStage(target: Vector3f)
    fun snapSlice()

    val newSlice: BlockingQueue<Pair<ServerSignal.Slice, ByteBuffer>>
    var latestServerStatus: ServerSignal.ServerStatus?

}