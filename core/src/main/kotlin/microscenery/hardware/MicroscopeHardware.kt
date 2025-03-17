package microscenery.hardware

import microscenery.signals.HardwareDimensions
import microscenery.signals.MicroscopeControlSignal
import microscenery.signals.MicroscopeSignal
import microscenery.signals.MicroscopeStatus
import org.joml.Vector3f
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Semaphore

interface MicroscopeHardware {
    var stagePosition: Vector3f
    fun goLive()

    /**
     * Stops all execution as fast as possible.
     */
    fun stop()
    fun snapSlice()
    fun shutdown()
    fun acquireStack(meta: MicroscopeControlSignal.AcquireStack)

    fun status(): MicroscopeStatus
    fun hardwareDimensions(): HardwareDimensions

    fun ablatePoints(signal: MicroscopeControlSignal.AblationPoints)
    fun startAcquisition()

    fun deviceSpecificCommands(data: ByteArray){}

    /**
     * Sync with microscope runtime. Blocks until microscope is in a free state again.
     *
     * (Using a Semaphore here since locks cant be released from non-owning threads.
     */
    fun sync(): Semaphore

    val output: BlockingQueue<MicroscopeSignal>
}

