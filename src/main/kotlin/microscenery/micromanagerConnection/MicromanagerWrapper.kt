package microscenery.micromanagerConnection

import graphics.scenery.utils.LazyLogger
import microscenery.Agent
import microscenery.MicroscenerySettings
import microscenery.MicroscopeHardware
import microscenery.network.*
import org.joml.Vector2i
import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import kotlin.properties.Delegates


class MicromanagerWrapper(
    private val mmConnection: MMConnection,
    var timeBetweenUpdates: Int = MicroscenerySettings.get("MMConnection.TimeBetweenStackAcquisition", 1000),
): Agent(), MicroscopeHardware{

    private val hardwareCommandsQueue = ArrayBlockingQueue<HardwareCommand>(5000)
    private val stopLock = Any()

    private var idCounter = 0
    var lastSnap = 0L


    override val output: BlockingQueue<ServerSignal> = ArrayBlockingQueue(10)


    private var status: ServerSignal.ServerStatus by Delegates.observable(
        ServerSignal.ServerStatus(
            ServerState.STARTUP, listOf(), 0, HardwareDimensions.EMPTY
        )
    ) { _, _, newStatus: ServerSignal.ServerStatus ->
        output.offer(newStatus)
    }

    override fun serverStatus(): ServerSignal.ServerStatus = status

    override var stagePosition: Vector3f
        get() = mmConnection.stagePosition
        set(value) {
            hardwareCommandsQueue.put(HardwareCommand.MoveStage(value, hardwareDimensions()))}


    var hardwareDimensions: HardwareDimensions //TODO send update on change
    override fun hardwareDimensions(): HardwareDimensions = hardwareDimensions

    init {
        val (stageMin,stageMax) = stageMinMax()
        mmConnection.updateSize()

        hardwareDimensions = HardwareDimensions(stageMin,stageMax,
            Vector2i(mmConnection.width,mmConnection.height),
        Vector3f(0.225f,0.225f,1.524f),// TODO get vertex size
        NumericType.INT16
        )

        startAgent()
        status = status.copy(state = ServerState.MANUAL, hwDimensions = hardwareDimensions)
    }


    override fun snapSlice() {
        hardwareCommandsQueue.put(HardwareCommand.SnapImage(false))
    }

    var live: Boolean
        get() = status.state == ServerState.LIVE
        set(value) {
            status = if (value) {
                hardwareCommandsQueue.put(HardwareCommand.SnapImage(true))
                status.copy(state = ServerState.LIVE)
            } else {

                synchronized(stopLock) {
                    hardwareCommandsQueue.clear()
                    hardwareCommandsQueue.put(HardwareCommand.Stop)
                }
                status.copy(state = ServerState.MANUAL)
            }
        }

    override fun shutdown() {
        status = status.copy(state = ServerState.SHUTTING_DOWN)
        synchronized(stopLock) {
            hardwareCommandsQueue.clear()
            hardwareCommandsQueue.put(HardwareCommand.Shutdown)
        }
    }

    private fun addToCommandQueueIfNotStopped(vararg commands: HardwareCommand) {
        synchronized(stopLock) {
            if (hardwareCommandsQueue.peek() !is HardwareCommand.Stop) {
                commands.forEach { hardwareCommandsQueue.put(it) }
            }
        }
    }

    override fun onLoop() {
            val hwCommand = hardwareCommandsQueue.poll()
            if (hwCommand == null) {
                Thread.sleep(200)
                return
            }
            when (hwCommand) {
                is HardwareCommand.GenerateStackCommands -> TODO()
                is HardwareCommand.MoveStage -> mmConnection.moveStage(hwCommand.safeTarget, false)
                is HardwareCommand.SnapImage -> {
                    val buf = MemoryUtil.memAlloc(this.status.hwDimensions.byteSize)
                    buf.clear()
                    if (lastSnap + timeBetweenUpdates > System.currentTimeMillis()) {
                        Thread.sleep(
                            (lastSnap + timeBetweenUpdates - System.currentTimeMillis()).coerceAtLeast(0)
                        )
                    }
                    mmConnection.snapSlice(buf.asShortBuffer())
                    val sliceSignal = ServerSignal.Slice(
                        idCounter++,
                        System.currentTimeMillis(),
                        mmConnection.stagePosition,
                        this.status.hwDimensions.byteSize,
                        hwCommand.stackId,
                        buf
                    )
                    output.put(sliceSignal)
                    lastSnap = System.currentTimeMillis()
                    if (hwCommand.live) {
                        addToCommandQueueIfNotStopped(hwCommand)
                    }
                }
                is HardwareCommand.Stop -> {} //it's just a marker
                is HardwareCommand.Shutdown -> {
                    this.close()
                }
        }
    }


    private fun stageMinMax(): Pair<Vector3f, Vector3f> {
        val min = Vector3f(
            MicroscenerySettings.get("Stage.minX"),
            MicroscenerySettings.get("Stage.minY"),
            MicroscenerySettings.get("Stage.minZ")
        )
        val max = Vector3f(
            MicroscenerySettings.get("Stage.maxX"),
            MicroscenerySettings.get("Stage.maxY"),
            MicroscenerySettings.get("Stage.maxZ")
        )
        if (min.x > max.x || min.y > max.y || min.z > max.z) {
            throw IllegalArgumentException("Min allowed stage area parameters need to be smaller than max values")
        }
        return min to max
    }

    private sealed class HardwareCommand {
        protected val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

        class MoveStage(target: Vector3f, hwd: HardwareDimensions) : HardwareCommand() {
            val safeTarget = Vector3f()

            init {
                for (i in 0..2) this.safeTarget.setComponent(i, target[i].coerceIn(hwd.stageMin[i], hwd.stageMax[i]))
                if (this.safeTarget != target) {
                    logger.warn("Had to coerce stage move parameters! From $target to ${this.safeTarget}")
                }
            }
        }

        data class SnapImage(val live: Boolean, val stackId: Int? = null) : HardwareCommand()
        data class GenerateStackCommands(val signal: ClientSignal.AcquireStack) : HardwareCommand()
        object Stop : HardwareCommand()
        object Shutdown : HardwareCommand()
    }
}
