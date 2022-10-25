package microscenery.hardware

import microscenery.signals.HardwareDimensions
import microscenery.signals.MicroscopeSignal
import microscenery.signals.MicroscopeStatus
import org.joml.Vector3f
import java.util.concurrent.BlockingQueue

interface MicroscopeHardware{
    var stagePosition: Vector3f
    fun snapSlice()
    fun shutdown()

    fun status(): MicroscopeStatus
    fun hardwareDimensions(): HardwareDimensions

    val output: BlockingQueue<MicroscopeSignal>
}

