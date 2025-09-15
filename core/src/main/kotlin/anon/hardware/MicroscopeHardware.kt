package anon.hardware

import anon.signals.HardwareDimensions
import anon.signals.MicroscopeControlSignal
import anon.signals.MicroscopeSignal
import anon.signals.MicroscopeStatus
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

