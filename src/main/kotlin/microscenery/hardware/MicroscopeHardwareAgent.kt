package microscenery.hardware

import microscenery.Agent
import microscenery.signals.HardwareDimensions
import microscenery.signals.MicroscopeSignal
import microscenery.signals.MicroscopeStatus
import microscenery.signals.ServerState
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

    override val output: BlockingQueue<MicroscopeSignal> = ArrayBlockingQueue(50)


    @Suppress("BlockingMethodInNonBlockingContext")
    protected var status: MicroscopeStatus by Delegates.observable(
        MicroscopeStatus(ServerState.STARTUP, Vector3f())
    ) { _, _, _ ->
        output.put(status)
    }


    protected var hardwareDimensions: HardwareDimensions by Delegates.observable(
        HardwareDimensions.EMPTY.copy(stageMin = Vector3f(-45f))
    ) { _, _, _ ->
        output.put(hardwareDimensions)
    }

    override fun status(): MicroscopeStatus = status
    override fun hardwareDimensions(): HardwareDimensions = hardwareDimensions
}