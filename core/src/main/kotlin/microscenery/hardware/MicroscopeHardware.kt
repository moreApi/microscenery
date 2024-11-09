package microscenery.hardware

import microscenery.signals.ClientSignal
import microscenery.signals.HardwareDimensions
import microscenery.signals.MicroscopeSignal
import microscenery.signals.MicroscopeStatus
import org.joml.Vector3f
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Future

interface MicroscopeHardware {
    var stagePosition: Vector3f
    fun goLive()

    /**
     * Stops all execution as fast as possible.
     */
    fun stop()
    fun snapSlice()
    fun shutdown()
    fun acquireStack(meta: ClientSignal.AcquireStack)

    fun status(): MicroscopeStatus
    fun hardwareDimensions(): HardwareDimensions

    fun ablatePoints(signal: ClientSignal.AblationPoints)
    fun startAcquisition()

    fun deviceSpecificCommands(data: ByteArray){}

    fun sync(): Future<Boolean>

    val output: BlockingQueue<MicroscopeSignal>
}

