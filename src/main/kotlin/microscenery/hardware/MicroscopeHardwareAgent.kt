package microscenery.hardware

import microscenery.Agent
import microscenery.network.HardwareDimensions
import microscenery.network.MicroscopeSignal
import microscenery.network.MicroscopeStatus
import microscenery.network.ServerState
import org.joml.Vector3f
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import kotlin.properties.Delegates

/**
 * Agent class to handle common microscopy handling stuff.
 *
 * !! ATTENTION!! Call [startAgent] eg. in the Init method of your class
 */
abstract class MicroscopeHardwareAgent(): Agent(), MicroscopeHardware {

    override val output: BlockingQueue<MicroscopeSignal> = ArrayBlockingQueue(10)

    protected var status: MicroscopeStatus by Delegates.observable(
        MicroscopeStatus(ServerState.STARTUP, Vector3f())
    ) { _, _, newStatus: MicroscopeStatus ->
        output.offer(newStatus)
    }

    protected var hardwareDimensions: HardwareDimensions by Delegates.observable(
        HardwareDimensions.EMPTY
    ) { _, _, value: HardwareDimensions ->
        output.offer(value)
    }

    override fun status(): MicroscopeStatus = status
    override fun hardwareDimensions(): HardwareDimensions = hardwareDimensions
}