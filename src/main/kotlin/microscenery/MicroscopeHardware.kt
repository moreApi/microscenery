package microscenery

import microscenery.network.HardwareDimensions
import microscenery.network.ServerSignal
import org.joml.Vector3f
import java.util.concurrent.BlockingQueue

interface MicroscopeHardware{

    var stagePosition:Vector3f

    fun serverStatus(): ServerSignal.ServerStatus
    fun hardwareDimensions(): HardwareDimensions
    fun snapSlice()
    fun shutdown()


    val output: BlockingQueue<ServerSignal>
}

