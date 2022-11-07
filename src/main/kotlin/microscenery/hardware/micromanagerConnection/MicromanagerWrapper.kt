package microscenery.hardware.micromanagerConnection

import graphics.scenery.utils.LazyLogger
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import microscenery.MicroscenerySettings
import microscenery.hardware.MicroscopeHardwareAgent
import microscenery.nowMillis
import microscenery.signals.*
import org.joml.Vector2i
import org.joml.Vector3f
import org.joml.Vector3i
import org.lwjgl.system.MemoryUtil
import java.util.concurrent.ArrayBlockingQueue
import kotlin.math.roundToInt


class MicromanagerWrapper(
    private val mmConnection: MMConnection,
    var timeBetweenUpdates: Int = MicroscenerySettings.get("MMConnection.TimeBetweenStackAcquisition", 1000),
) : MicroscopeHardwareAgent() {
    protected val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private val hardwareCommandsQueue = ArrayBlockingQueue<HardwareCommand>(5000)

    // this lock is only relevant for self replicating commands e.g. snapSlice(live=true)
    private val stopLock = Any()

    private var idCounter = 0
    var lastSnap = 0L

    init {
        updateHardwareDimensions()

        startAgent()
        status = status.copy(state = ServerState.MANUAL)
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
            Vector3f(0.225f, 0.225f, 1.524f),// TODO get vertex size
            NumericType.INT16
        )
    }

    override fun snapSlice() {
        hardwareCommandsQueue.put(HardwareCommand.SnapImage(false))
    }

    override fun moveStage(target: Vector3f) {
        hardwareCommandsQueue.put(HardwareCommand.MoveStage(target, hardwareDimensions, true))
    }

    override fun acquireStack(meta: ClientSignal.AcquireStack) {
        hardwareCommandsQueue.put(HardwareCommand.GenerateStackCommands(meta))
    }

    override fun live(isLive: Boolean) {
        status = if (isLive) {
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
            // if not busy update stage position. It might have been moved via external inputs.
            if (stagePosition != mmConnection.stagePosition){
                stagePosition = mmConnection.stagePosition
            }
            Thread.sleep(200)
            return
        }
        when (hwCommand) {
            is HardwareCommand.GenerateStackCommands -> {
                val meta = hwCommand.signal

                val start = hardwareDimensions.coercePosition(meta.startPosition, logger)
                val end = hardwareDimensions.coercePosition(meta.endPosition,logger)
                val dist = end - start
                val steps = (dist.length() / meta.stepSize).roundToInt()
                val step = dist * (1f / steps)

                val currentStack = Stack(
                    idCounter++,
                    false,
                    start,
                    Vector3i(hardwareDimensions.imageSize, steps),
                    nowMillis(),
                    hardwareDimensions.vertexSize
                )
                output.put(currentStack)
                status = status.copy(state = ServerState.STACK)

                for (i in 0 until steps) {
                    hardwareCommandsQueue.put(HardwareCommand.MoveStage(start + (step * i.toFloat()),hardwareDimensions,true))
                    hardwareCommandsQueue.put(HardwareCommand.SnapImage(false,currentStack.Id))
                }
            }
            is HardwareCommand.MoveStage -> {
                mmConnection.moveStage(hwCommand.safeTarget, hwCommand.waitForCompletion)
                status = status.copy(stagePosition = hwCommand.safeTarget)
            }
            is HardwareCommand.SnapImage -> {
                val buf = MemoryUtil.memAlloc(hardwareDimensions.byteSize)
                buf.clear()
                if (lastSnap + timeBetweenUpdates > System.currentTimeMillis()) {
                    //TODO handle incomming stage move events, in case of live
                    Thread.sleep(
                        (lastSnap + timeBetweenUpdates - System.currentTimeMillis()).coerceAtLeast(0)
                    )
                }
                mmConnection.snapSlice(buf.asShortBuffer())
                val sliceSignal = Slice(
                    idCounter++,
                    System.currentTimeMillis(),
                    mmConnection.stagePosition,
                    hardwareDimensions.byteSize,
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

        class MoveStage(target: Vector3f, hwd: HardwareDimensions, val waitForCompletion: Boolean = false) :
            HardwareCommand() {
            val safeTarget = hwd.coercePosition(target,logger)
        }

        data class SnapImage(val live: Boolean, val stackId: Int? = null) : HardwareCommand()
        data class GenerateStackCommands(val signal: ClientSignal.AcquireStack) : HardwareCommand()
        object Stop : HardwareCommand()
        object Shutdown : HardwareCommand()
    }
}
