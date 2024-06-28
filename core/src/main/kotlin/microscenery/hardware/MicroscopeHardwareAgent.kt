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
abstract class MicroscopeHardwareAgent : Agent(), MicroscopeHardware {

    override val output: BlockingQueue<MicroscopeSignal> = ArrayBlockingQueue(50)


    protected var status: MicroscopeStatus by Delegates.observable(
        MicroscopeStatus(ServerState.STARTUP, Vector3f(), false)
    ) { _, _, _ ->
        output.put(status)
    }

    override var stagePosition: Vector3f
        get() = status.stagePosition
        set(value) {
            moveStage(value)
        }

    protected var hardwareDimensions: HardwareDimensions by Delegates.observable(
        HardwareDimensions.EMPTY.copy(stageMin = Vector3f(-45f))
    ) { _, old, new ->
        if (old != new) output.put(hardwareDimensions)
    }

    override fun status(): MicroscopeStatus = status
    override fun hardwareDimensions(): HardwareDimensions = hardwareDimensions

    /**
     * New stage position has to be announced in status in implementing method.
     */
    protected abstract fun moveStage(target: Vector3f)
}