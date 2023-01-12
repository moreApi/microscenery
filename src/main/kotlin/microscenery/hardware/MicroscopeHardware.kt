package microscenery.hardware

import microscenery.signals.ClientSignal
import microscenery.signals.HardwareDimensions
import microscenery.signals.MicroscopeSignal
import microscenery.signals.MicroscopeStatus
import org.joml.Vector3f
import java.util.concurrent.BlockingQueue

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

    val output: BlockingQueue<MicroscopeSignal>
}

