package microscenery

import graphics.scenery.utils.LazyLogger
import kotlinx.event.event
import microscenery.network.*
import mmcorej.CMMCore
import org.joml.Vector2i
import org.joml.Vector3f
import org.zeromq.ZContext
import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread
import kotlin.properties.Delegates

// TODO MAYBE: Put hardware commands worker in own class
@Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")
class ControlledVolumeStreamServer @JvmOverloads constructor(
    core: CMMCore? = null,
    val basePort: Int = MicroscenerySettings.get("Network.basePort"),
    val connections: Int = MicroscenerySettings.get("Network.connections"),
    var timeBetweenUpdates: Int = MicroscenerySettings.get("MMConnection.TimeBetweenStackAcquisition", 1000),
    private val zContext: ZContext = microscenery.zContext
) {
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))
    val mmConnection = MMConnection(core_ = core)
    private val controlConnection = ControlSignalsServer(zContext, basePort)

    val storage = SliceStorage()
    val dataSender = BiggishDataServer(basePort + 1, storage, zContext)

    private val stopLock = Any()
    private val hardwareCommandsQueue = ArrayBlockingQueue<HardwareCommand>(5000)

    var running = false
    var workerThread: Thread? = null

    var idCounter = 0

    val statusChange = event<ServerSignal.ServerStatus>()
    var status by Delegates.observable(
        ServerSignal.ServerStatus(
            ServerState.STARTUP, listOf(dataSender.port), controlConnection.connectedClients, HardwareDimensions(
                stageMinMax().first, stageMinMax().second, Vector2i(), Vector3f(), // TODO
                NumericType.INT16
            )
        )
    ) { _, _, newStatus: ServerSignal.ServerStatus ->
        statusChange(newStatus)
    }
        private set

    init {
        if (connections != 1) logger.warn("More than one data connection are currently not supported. Config asks for $connections")

        statusChange += {
            controlConnection.sendSignal(it)
        }

        controlConnection.addListener(this::processClientSignal)

        mmConnection.updateSize()
        status = status.copy(
            hwDimensions = status.hwDimensions.copy(
                imageSize = Vector2i(mmConnection.width, mmConnection.height)
            )
        )

        workerThread = initWorkerThread()

        status = status.copy(
            state = ServerState.MANUAL
        )
    }

    private fun processClientSignal(it: ClientSignal) {
        when (it) {
            is ClientSignal.AcquireStack -> hardwareCommandsQueue.put(HardwareCommand.GenerateStackCommands(it))
            ClientSignal.ClientSignOn -> controlConnection.sendSignal(status)
            is ClientSignal.Live -> {
                hardwareCommandsQueue.put(HardwareCommand.SnapImage(true))
                status = status.copy(state = ServerState.LIVE)
            }
            is ClientSignal.MoveStage -> {
                hardwareCommandsQueue.put(HardwareCommand.MoveStage(it.target, status.hwDimensions))
            }
            ClientSignal.Shutdown -> {
                status = status.copy(state = ServerState.SHUTTING_DOWN)
                val closingSenderThread = dataSender.close()
                synchronized(stopLock) {
                    hardwareCommandsQueue.clear()
                    hardwareCommandsQueue.put(HardwareCommand.Shutdown)
                }
                closingSenderThread.join()
                zContext.destroy()
            }
            ClientSignal.SnapImage -> {
                hardwareCommandsQueue.put(HardwareCommand.SnapImage(false))
            }
            ClientSignal.Stop -> {
                synchronized(stopLock) {
                    hardwareCommandsQueue.clear()
                    hardwareCommandsQueue.put(HardwareCommand.Stop)
                }
                status = status.copy(state = ServerState.MANUAL)
            }
        }
    }

    private fun addToCommandQueueIfNotStopped(vararg commands: HardwareCommand) {
        synchronized(stopLock) {
            if (hardwareCommandsQueue.peek() !is HardwareCommand.Stop) {
                commands.forEach { hardwareCommandsQueue.put(it) }
            }
        }
    }

    private fun initWorkerThread() = thread {
        var lastSnap = 0L
        while (running) {
            val hwCommand = hardwareCommandsQueue.poll()
            if (hwCommand == null) {
                Thread.sleep(200)
                continue
            }
            when (hwCommand) {
                is HardwareCommand.GenerateStackCommands -> TODO()
                is HardwareCommand.MoveStage -> mmConnection.moveStage(hwCommand.target, false)
                is HardwareCommand.SnapImage -> {
                    val buf = storage.newSlice(this.status.hwDimensions.byteSize)
                    buf.clear()
                    if (lastSnap + timeBetweenUpdates > System.currentTimeMillis()) {
                        Thread.sleep(
                            (lastSnap + timeBetweenUpdates - System.currentTimeMillis()).coerceAtLeast(0)
                        )
                    }
                    mmConnection.snapSlice(buf.asShortBuffer())
                    val metadata = ServerSignal.Slice(
                        idCounter++,
                        System.currentTimeMillis(),
                        mmConnection.stagePosition,
                        this.status.hwDimensions.byteSize,
                        hwCommand.stackId
                    )
                    storage.addSlice(metadata.Id, buf)
                    controlConnection.sendSignal(metadata)
                    lastSnap = System.currentTimeMillis()
                    if (hwCommand.live) {
                        addToCommandQueueIfNotStopped(hwCommand)
                    }
                }
                is HardwareCommand.Stop -> {} //it's just a marker
                is HardwareCommand.Shutdown -> {
                    running = false
                    break
                }
            }
        }
        microscenery.zContext.destroy()
    }

    private sealed class HardwareCommand {
        protected val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

        class MoveStage(target: Vector3f, hwd: HardwareDimensions) : HardwareCommand() {
            val target = Vector3f()

            init {
                for (i in 0..2) this.target.setComponent(i, target.x.coerceIn(hwd.stageMin[i], hwd.stageMax[i]))
                if (this.target != target) {
                    logger.warn("Had to coerce stage move parameters! From $target to ${this.target}")
                }
            }
        }

        data class SnapImage(val live: Boolean, val stackId: Int? = null) : HardwareCommand()
        data class GenerateStackCommands(val signal: ClientSignal.AcquireStack) : HardwareCommand()
        object Stop : HardwareCommand()
        object Shutdown : HardwareCommand()
    }

    @Suppress("unused")
    fun stop() {
        logger.info("Got stop Command")
        controlConnection.sendInternalSignals(listOf(ClientSignal.Stop))
    }

    @Suppress("unused")
    fun shutdown() {
        logger.info("Got Stop Command")
        controlConnection.sendInternalSignals(listOf(ClientSignal.Shutdown))
    }

    /**
     * Access settings. Java compatability function
     */
    @Suppress("unused")
    fun getSettings() = MicroscenerySettings


    companion object {
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
    }
}