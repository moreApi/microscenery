package microscenery.hardware.micromanagerConnection

import graphics.scenery.utils.LazyLogger
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import graphics.scenery.utils.extensions.xy
import microscenery.*
import microscenery.hardware.MicroscopeHardwareAgent
import microscenery.signals.*
import org.joml.Vector2i
import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil
import java.util.concurrent.ArrayBlockingQueue
import kotlin.math.roundToInt

/**
 * Wrapper of a [MMConnection] to fit the MicroscopeHardware interface.
 * Queues incoming commands
 *
 * @param disableStagePosUpdates if true the position of the stage is only updated on move commands. Makes status updates more deterministic for tests.
 */
class MicromanagerWrapper(
    private val mmConnection: MMConnection,
    var timeBetweenUpdates: Int = MicroscenerySettings.get("MMConnection.TimeBetweenStackAcquisition", 1000),
    val disableStagePosUpdates: Boolean = false
) : MicroscopeHardwareAgent() {
    protected val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private val hardwareCommandsQueue = ArrayBlockingQueue<HardwareCommand>(5000)

    // this lock is only relevant for self replicating commands e.g. snapSlice(live=true)
    private val stopLock = Any()

    private var idCounter = 0
    var lastSnap = 0L
    var vertexDiameter = MicroscenerySettings.get("MMConnection.vertexDiameter", mmConnection.pixelSizeUm)
        set(value) {
            field = value
            updateHardwareDimensions()
        }

    init {
        MicroscenerySettings.setVector3fIfUnset("Stage.min",mmConnection.stagePosition)
        MicroscenerySettings.setVector3fIfUnset("Stage.max",mmConnection.stagePosition)

        updateHardwareDimensions()

        if (hardwareDimensions.coercePosition(mmConnection.stagePosition, null) != mmConnection.stagePosition){
            val msg = "Stage ${mmConnection.stagePosition.toReadableString()} not in allowed area " +
                    "from ${MicroscenerySettings.getVector3("Stage.min")?.toReadableString()}"
                    "to ${MicroscenerySettings.getVector3("Stage.max")?.toReadableString()}. Aborting!"
            logger.error(msg)
            throw IllegalStateException(msg)
        }

        startAgent()
        status = status.copy(stagePosition = mmConnection.stagePosition, state = ServerState.MANUAL)
    }

    /**
     * Reads settings and image size to update hardware dimensions.
     *
     * Takes one image with the microscope to make sure, image meta information are correctly set.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun updateHardwareDimensions() {
        val (stageMin, stageMax) = stageMinMax()
        mmConnection.updateSize()

        hardwareDimensions = HardwareDimensions(
            stageMin, stageMax,
            Vector2i(mmConnection.width, mmConnection.height),
            vertexDiameter,
            NumericType.INT16
        )
    }

    override fun snapSlice() {
        hardwareCommandsQueue.add(HardwareCommand.SnapImage(false))
    }

    override fun moveStage(target: Vector3f) {
        hardwareCommandsQueue.add(HardwareCommand.MoveStage(target, hardwareDimensions, true))
    }

    override fun acquireStack(meta: ClientSignal.AcquireStack) {
        stop()
        hardwareCommandsQueue.add(HardwareCommand.GenerateStackCommands(meta))
    }

    override fun goLive() {
        hardwareCommandsQueue.add(HardwareCommand.SnapImage(true))
    }

    override fun stop() {
        synchronized(stopLock) {
            hardwareCommandsQueue.clear()
            hardwareCommandsQueue.add(HardwareCommand.Stop)
        }
    }

    override fun shutdown() {
        status = status.copy(state = ServerState.SHUTTING_DOWN)
        synchronized(stopLock) {
            hardwareCommandsQueue.clear()
            hardwareCommandsQueue.add(HardwareCommand.Shutdown)
        }
    }

    private fun addToCommandQueueIfNotStopped(vararg commands: HardwareCommand) {
        synchronized(stopLock) {
            if (hardwareCommandsQueue.peek() !is HardwareCommand.Stop) {
                commands.forEach { hardwareCommandsQueue.add(it) }
            }
        }
    }

    override fun onLoop() {
        val hwCommand = hardwareCommandsQueue.poll()
        if (hwCommand == null) {
            // if not busy update stage position. It might have been moved via external inputs.
            mmConnection.stagePosition.let {
                if (stagePosition != it && status.state != ServerState.STARTUP && !disableStagePosUpdates) {
                    status = status.copy(stagePosition = it)
                }
            }
            Thread.sleep(200)
            return
        }
        when (hwCommand) {
            is HardwareCommand.GenerateStackCommands -> {
                val meta = hwCommand.signal

                val start = hardwareDimensions.coercePosition(meta.startPosition, logger)
                val end = hardwareDimensions.coercePosition(meta.endPosition, logger)
                val dist = end - start
                val steps = (dist.length() / meta.stepSize).roundToInt()
                val step = dist * (1f / steps)

                val currentStack = Stack(
                    idCounter++,
                    false,
                    start,
                    end,
                    steps,
                    nowMillis()
                )
                output.put(currentStack)
                status = status.copy(state = ServerState.STACK)

                for (i in 0 until steps) {
                    hardwareCommandsQueue.add(
                        HardwareCommand.MoveStage(
                            start + (step * i.toFloat()),
                            hardwareDimensions,
                            true
                        )
                    )
                    hardwareCommandsQueue.add(HardwareCommand.SnapImage(false, currentStack.Id to i))
                }
            }
            is HardwareCommand.MoveStage -> {
                // skip if next command is also move
                if (hardwareCommandsQueue.peek() is HardwareCommand.MoveStage) return

                // precision check, if movement below stage precision, skip
                val target = hwCommand.safeTarget
                val overXYPrecision = MicroscenerySettings.getOrNull<Float>("Stage.precisionXY")?.let {
                    !stagePosition.xy().equals(target.xy(), it)
                } ?: true
                val overZPrecision = MicroscenerySettings.getOrNull<Float>("Stage.precisionZ")?.let { precision ->
                    val from = stagePosition.z
                    val to = target.z
                    to < from - precision || from + precision < to
                } ?: true
                if (!overXYPrecision && !overZPrecision){
                    logger.info("Not moving stage to ${hwCommand.safeTarget.toReadableString()} because " +
                            "to close to stage pos ${stagePosition.toReadableString()}")
                    return
                }

                try {
                    mmConnection.moveStage(hwCommand.safeTarget, hwCommand.waitForCompletion)
                } catch (t: Throwable) {
                    logger.warn("Failed move command to ${hwCommand.safeTarget}", t)
                    return
                }
                status = status.copy(stagePosition = hwCommand.safeTarget)
            }
            is HardwareCommand.SnapImage -> {
                if (hwCommand.live && lastSnap + timeBetweenUpdates > System.currentTimeMillis()) {
                    if (hardwareCommandsQueue.isEmpty()) {
                        Thread.sleep(
                            (lastSnap + timeBetweenUpdates - System.currentTimeMillis()).coerceAtLeast(0)
                        )
                    } else {
                        hardwareCommandsQueue.add(hwCommand)
                        return
                    }
                }
                val buf = MemoryUtil.memAlloc(hardwareDimensions.byteSize)
                buf.clear()
                try {
                    mmConnection.snapSlice(buf.asShortBuffer())
                } catch (t: Throwable) {
                    logger.warn("Failed snap command", t)
                    return
                }
                val sliceSignal = Slice(
                    idCounter++,
                    System.currentTimeMillis(),
                    mmConnection.stagePosition,
                    hardwareDimensions.byteSize,
                    hwCommand.stackIdAndSliceIndex,
                    buf
                )
                output.put(sliceSignal)
                lastSnap = System.currentTimeMillis()
                if (hwCommand.live) {
                    when (status.state) {
                        ServerState.LIVE -> {}
                        ServerState.MANUAL -> status = status.copy(state = ServerState.LIVE)
                        else -> {
                            stop()
                            throw IllegalStateException("Want to go live but server is ${status.state}. Stopping.")
                        }
                    }
                    addToCommandQueueIfNotStopped(hwCommand)
                }
            }
            is HardwareCommand.Stop -> {
                status = status.copy(state = ServerState.MANUAL)
            }
            is HardwareCommand.Shutdown -> {
                this.close()
            }
        }
    }

    private fun stageMinMax(): Pair<Vector3f, Vector3f> {
        val min = Vector3f(
            MicroscenerySettings.get("Stage.minX", stagePosition.x),
            MicroscenerySettings.get("Stage.minY", stagePosition.y),
            MicroscenerySettings.get("Stage.minZ", stagePosition.z)
        )
        val max = Vector3f(
            MicroscenerySettings.get("Stage.maxX", stagePosition.x),
            MicroscenerySettings.get("Stage.maxY", stagePosition.y),
            MicroscenerySettings.get("Stage.maxZ", stagePosition.z)
        )
        if (min.x > max.x || min.y > max.y || min.z > max.z) {
            throw IllegalArgumentException("Min allowed stage area parameters need to be smaller than max values")
        }
        return min to max
    }

    private sealed class HardwareCommand {
        protected val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

        class MoveStage(target: Vector3f, hwd: HardwareDimensions, val waitForCompletion: Boolean = false) :
            HardwareCommand() {
            val safeTarget = hwd.coercePosition(target, logger)
        }

        data class SnapImage(val live: Boolean, val stackIdAndSliceIndex: Pair<Int, Int>? = null) : HardwareCommand()
        data class GenerateStackCommands(val signal: ClientSignal.AcquireStack) : HardwareCommand()
        object Stop : HardwareCommand()
        object Shutdown : HardwareCommand()
    }
}
