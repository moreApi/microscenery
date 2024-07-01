package microscenery.hardware.micromanagerConnection

import com.google.protobuf.InvalidProtocolBufferException
import fromScenery.lazyLogger
import fromScenery.utils.extensions.minus
import fromScenery.utils.extensions.plus
import fromScenery.utils.extensions.times
import fromScenery.utils.extensions.xy
import me.jancasus.microscenery.network.v2.MicroManagerSignal
import microscenery.*
import microscenery.hardware.MicroscopeHardwareAgent
import microscenery.signals.*
import org.joml.Vector2i
import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil
import java.nio.Buffer
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.system.measureNanoTime

/**
 * Wrapper of a [MMCoreConnector] to fit the MicroscopeHardware interface.
 * Queues incoming commands
 */
class MicromanagerWrapper(
    private val mmCoreConnector: MMCoreConnector,
    private val mmStudioConnector: MMStudioConnector = DummyMMStudioConnector(),
) : MicroscopeHardwareAgent() {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private val hardwareCommandsQueue = ArrayBlockingQueue<HardwareCommand>(5000)

    // this lock is only relevant for self replicating commands e.g. snapSlice(live=true)
    private val stopLock = Any()

    private var ablationThread: Thread? = null
    private var stagePositionPollingThread: Thread? = null

    // for Slices and stacks
    private var idCounter = 0
    var vertexDiameter = MicroscenerySettings.get(Settings.MMMicroscope.VertexDiameter, mmCoreConnector.pixelSizeUm)
        set(value) {
            field = value
            updateHardwareDimensions()
        }

    init {
        MicroscenerySettings.setIfUnset(Settings.Stage.Limits.OriginMoveProtection, true)
        MicroscenerySettings.setVector3fIfUnset(Settings.Stage.Limits.Min, mmCoreConnector.stagePosition)
        MicroscenerySettings.setVector3fIfUnset(Settings.Stage.Limits.Max, mmCoreConnector.stagePosition)
        MicroscenerySettings.setIfUnset(Settings.MMMicroscope.PollStagePositionFrequencyMS, 0)
        MicroscenerySettings.setIfUnset(Settings.Stage.Limits.AutoGrowStageLimits, true)

        MicroscenerySettings.addUpdateRoutine(Settings.MMMicroscope.PollStagePositionFrequencyMS, true) {
            updateStagePositionPollingThread()
        }

        updateHardwareDimensions()

        if (hardwareDimensions.coercePosition(mmCoreConnector.stagePosition, null) != mmCoreConnector.stagePosition) {
            when (mmStudioConnector.askForStageLimitErrorResolve()) {
                MMStudioConnector.StageLimitErrorResolves.RESET_LIMITS -> {
                    hardwareDimensions = hardwareDimensions.copy(
                        stageMin = stagePosition,
                        stageMax = stagePosition,
                    )
                    MicroscenerySettings.setVector3f(Settings.Stage.Limits.Max, stagePosition)
                    MicroscenerySettings.setVector3f(Settings.Stage.Limits.Min, stagePosition)
                }

                MMStudioConnector.StageLimitErrorResolves.MOVE_STAGE -> stagePosition =
                    (hardwareDimensions().stageMin + hardwareDimensions().stageMax).times(0.5f)

                MMStudioConnector.StageLimitErrorResolves.IGNORE -> {}
                null -> throw IllegalArgumentException(
                    "Stage out of bounds. Select resolve in error dialog or" +
                            "delete stage limits in properties. Aborting here to not crush equipment."
                )
            }
        }

        startAgent()
        status = status.copy(stagePosition = mmCoreConnector.stagePosition, state = ServerState.MANUAL)
    }

    private fun updateStagePositionPollingThread() {
        val freq = MicroscenerySettings.get(Settings.MMMicroscope.PollStagePositionFrequencyMS, 0)
        if (freq == 0) {
            stagePositionPollingThread?.interrupt()
        } else {
            stagePositionPollingThread?.interrupt()
            stagePositionPollingThread?.join()
            stagePositionPollingThread = thread(isDaemon = true, name = "MicromanagerWrapperStagePositionPoller") {
                while (!Thread.currentThread().isInterrupted) {
                    val newPos = mmCoreConnector.stagePosition
                    if (newPos != stagePosition) {
                        updateStagePositionNoMovement(newPos)
                    }
                    Thread.sleep(freq.toLong())
                }
            }
        }
    }

    /**
     * Reads settings and image size to update hardware dimensions.
     *
     * Takes one image with the microscope to make sure, image meta information are correctly set.
     * Can be called from UI.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun updateHardwareDimensions() {
        val (stageMin, stageMax) = stageMinMax()
        mmCoreConnector.updateSize()

        hardwareDimensions = HardwareDimensions(
            stageMin, stageMax,
            Vector2i(mmCoreConnector.width, mmCoreConnector.height),
            vertexDiameter,
            NumericType.INT16
        )
    }

    //############################## called from external threads ##############################
    // to following functions are called from external threads and not from this agents thread

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

    override fun ablatePoints(signal: ClientSignal.AblationPoints) {
        val points = signal.points.map {
            it.copy(position = hardwareDimensions.coercePosition(it.position, logger))
        }
        hardwareCommandsQueue.add(HardwareCommand.AblatePoints(points))
    }

    override fun goLive() {
        hardwareCommandsQueue.add(HardwareCommand.SnapImage(true))
    }

    override fun stop() {
        ablationThread?.interrupt()
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

    override fun startAcquisition() {
        hardwareCommandsQueue.add(HardwareCommand.StartAcquisition)
    }

    override fun deviceSpecificCommands(data: ByteArray) {
        val signal = try {
            MicroManagerSignal.parseFrom(data)
        } catch (e: InvalidProtocolBufferException) {
            logger.error("Could not parse deviceSpecificCommands: $data")
            return
        }
        when (signal.signalCase ?: throw IllegalArgumentException("Illegal payload")) {
            MicroManagerSignal.SignalCase.SIGNAL_NOT_SET ->
                throw IllegalArgumentException("Signal is not set in Client signal message")

            MicroManagerSignal.SignalCase.ADDTOPOSITIONLIST -> {
                val pos = signal.addToPositionList
                mmStudioConnector.addPositionToPositionList(pos.label, pos.pos.toPoko())
            }
        }
    }

    /**
     * Sets the stage position to [pos] in memory but does not issue commands to the hardware.
     * Used for updates from the hardware cause by manual movement by the user.
     */
    @Suppress("unused")
    fun updateStagePositionNoMovement(pos: Vector3f) {
        if (status.state != ServerState.STARTUP) {
            status = status.copy(stagePosition = pos)

            if (MicroscenerySettings.get(Settings.Stage.Limits.AutoGrowStageLimits, true)) {

                val min = hardwareDimensions.stageMin
                val max = hardwareDimensions.stageMax

                val newMin = Vector3f(min)
                val newMax = Vector3f(max)

                for (i in 0..2) {
                    newMin.setComponent(i, min(pos[i], min[i]))
                    newMax.setComponent(i, max(pos[i], max[i]))
                }
                if (min != newMin || max != newMax) {
                    hardwareDimensions = hardwareDimensions.copy(stageMin = newMin, stageMax = newMax)
                }
            }
        }
    }

    /**
     * Send data as slice.
     * Used for data acquired by use of the traditional micromanager interface.
     */
    @Suppress("unused")
    fun externalSnap(position: Vector3f, data: ByteBuffer) {
        updateHardwareDimensions()
        val sliceSignal = Slice(
            idCounter++,
            System.currentTimeMillis(),
            position,
            hardwareDimensions.byteSize,
            null,
            data
        )
        output.put(sliceSignal)
    }

    //############################## end of called from external threads ##############################

    override fun onLoop() {
        when (val hwCommand = hardwareCommandsQueue.poll(200, TimeUnit.MILLISECONDS)) {
            is HardwareCommand.GenerateStackCommands -> {
                executeGenerateStackCommands(hwCommand)
            }

            is HardwareCommand.MoveStage -> {
                executeMoveStage(hwCommand.safeTarget, hwCommand.waitForCompletion)
            }

            is HardwareCommand.SnapImage -> {
                executeSnapImage(hwCommand)
            }

            is HardwareCommand.Stop -> {
                mmStudioConnector.live(false)
                status = status.copy(state = ServerState.MANUAL)
            }

            is HardwareCommand.Shutdown -> {
                this.close()
            }

            is HardwareCommand.AblatePoints -> {
                executeAblatePoints(hwCommand)
            }

            HardwareCommand.StartAcquisition -> mmStudioConnector.startAcquisition()
        }
    }

    private fun executeSnapImage(hwCommand: HardwareCommand.SnapImage) {
        if (hwCommand.live) {
            mmStudioConnector.live(true)
            when (status.state) {
                ServerState.LIVE -> {}
                ServerState.MANUAL -> status = status.copy(state = ServerState.LIVE)
                else -> {
                    stop()
                    throw IllegalStateException("Want to go live but server is ${status.state}. Stopping.")
                }
            }
            return
        }

        fun takeImage(): ByteBuffer {
            val buf = MemoryUtil.memAlloc(hardwareDimensions.byteSize)
            (buf as Buffer).clear() // this cast has to be done to be compatible with JDK 8
            try {
                mmCoreConnector.snapSlice(buf.asShortBuffer())
            } catch (t: Throwable) {
                MemoryUtil.memFree(buf)
                throw t
            }
            return buf
        }

        val buf = try {
            takeImage()
        } catch (t: Throwable) {
            //image size might has be changed by ROI selection -> retry
            updateHardwareDimensions()
            try {
                takeImage()
            } catch (t: Throwable) {
                logger.warn("Failed snap command", t)
                throw t
            }
        }

        val sliceSignal = Slice(
            idCounter++,
            System.currentTimeMillis(),
            mmCoreConnector.stagePosition,
            hardwareDimensions.byteSize,
            hwCommand.stackIdAndSliceIndex,
            buf
        )
        output.put(sliceSignal)
    }

    private fun executeGenerateStackCommands(hwCommand: HardwareCommand.GenerateStackCommands) {
        val meta = hwCommand.signal

        val start = hardwareDimensions.coercePosition(meta.startPosition, logger)
        val end = hardwareDimensions.coercePosition(meta.endPosition, logger)
        val dist = end - start
        val steps = (dist.length() / meta.stepSize).roundToInt()
        val step = dist * (1f / steps)

        val currentStack = Stack(
            if (meta.id > 0) meta.id else idCounter++,
            meta.live,
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

        if (hwCommand.signal.live) {
            addToCommandQueueIfNotStopped(
                HardwareCommand.GenerateStackCommands(
                    signal = meta.copy(id = currentStack.Id)
                )
            )
        }
    }

    private fun executeAblatePoints(hwCommand: HardwareCommand.AblatePoints) {
        //this is a thread so it can be interrupted asynchronously
        var totalTime = 0
        val perPointTime = mutableListOf<Int>()
        val debug = MicroscenerySettings.get("debug", false)
        ablationThread = thread(isDaemon = true) {
            status = status.copy(state = ServerState.ABLATION)
            mmCoreConnector.ablationShutter(true, true)
            try {
                totalTime = measureNanoTime {
                    hwCommand.points.forEach { point ->
                        if (Thread.currentThread().isInterrupted) {
                            return@thread
                        }
                        logger.info("Executing $point")
                        val startAblation = System.nanoTime()
                        val moveTime = measureNanoTime {
                            if (debug) {
                                executeMoveStage(point.position, true)
                            } else {
                                mmCoreConnector.moveStage(point.position, true)
                            }
                        }
                        if (point.laserOn) {
                            mmCoreConnector.laserPower(point.laserPower)
                        }
                        val sleepTime = point.dwellTime - if (point.countMoveTime) moveTime else 0
                        if (sleepTime < 0 && point.dwellTime > 0) {
                            logger.warn(
                                "Ablation: stage movement was longer than dwell time. " + "$moveTime ns > ${point.dwellTime} ns"
                            )
                        } else if (sleepTime > 0) {
                            Thread.sleep(sleepTime.nanosToMillis())
                        }
                        if (point.laserOff) {
                            mmCoreConnector.laserPower(0f)
                        }
                        perPointTime.add((System.nanoTime() - startAblation).nanosToMillis().toInt())
                    }
                }.nanosToMillis().toInt()
            } catch (_: InterruptedException) {
            } finally {
                mmCoreConnector.laserPower(0f)
                mmCoreConnector.ablationShutter(false, true)
            }
        }
        ablationThread?.join()
        mmCoreConnector.laserPower(0f)
        mmCoreConnector.ablationShutter(false, true)
        status = status.copy(state = ServerState.MANUAL)
        output.put(AblationResults(totalTime, perPointTime))
    }

    private fun executeMoveStage(target: Vector3f, wait: Boolean) {
        // skip if next command is also move
        if (hardwareCommandsQueue.peek() is HardwareCommand.MoveStage) return

        // precision check, if movement below stage precision, skip
        val overXYPrecision = MicroscenerySettings.getOrNull<Float>(Settings.Stage.PrecisionXY)?.let {
            !stagePosition.xy().equals(target.xy(), it)
        } ?: true
        val overZPrecision = MicroscenerySettings.getOrNull<Float>(Settings.Stage.PrecisionZ)?.let { precision ->
            val from = stagePosition.z
            val to = target.z
            to < from - precision || from + precision < to
        } ?: true
        if (!overXYPrecision && !overZPrecision) {
            logger.info(
                "Not moving stage to ${target.toReadableString()} because " +
                        "to close to stage pos ${stagePosition.toReadableString()}"
            )
            return
        }


        if (MicroscenerySettings.get(Settings.Stage.Limits.OriginMoveProtection, true)
            && target == Vector3f(0f)
        ) {//( target.x == 0f || target.y == 0f || target.z == 0f)){
            logger.warn("Ignoring stage move command because Settings.Stage.Limits.OriginMoveProtection is true")
            return
        }

        try {
            mmCoreConnector.moveStage(target, wait)
        } catch (t: Throwable) {
            logger.warn("Failed move command to ${target.toReadableString()}", t)
            return
        }

        status = status.copy(stagePosition = target)
    }


    private fun addToCommandQueueIfNotStopped(vararg commands: HardwareCommand) {
        synchronized(stopLock) {
            if (hardwareCommandsQueue.peek() !is HardwareCommand.Stop) {
                commands.forEach { hardwareCommandsQueue.add(it) }
            }
        }
    }


    private fun stageMinMax(): Pair<Vector3f, Vector3f> {
        val min = MicroscenerySettings.getVector3(Settings.Stage.Limits.Min, stagePosition)!!
        val max = MicroscenerySettings.getVector3(Settings.Stage.Limits.Max, stagePosition)!!
        if (min.x > max.x || min.y > max.y || min.z > max.z) {
            throw IllegalArgumentException("Min allowed stage area parameters need to be smaller than max values")
        }
        return min to max
    }

    private sealed class HardwareCommand {
        protected val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

        class MoveStage(target: Vector3f, hwd: HardwareDimensions, val waitForCompletion: Boolean = false) :
            HardwareCommand() {
            val safeTarget = hwd.coercePosition(target, logger)
        }

        data class SnapImage(val live: Boolean, val stackIdAndSliceIndex: Pair<Int, Int>? = null) : HardwareCommand()
        data class GenerateStackCommands(val signal: ClientSignal.AcquireStack) : HardwareCommand()
        object Stop : HardwareCommand()
        object Shutdown : HardwareCommand()
        data class AblatePoints(val points: List<ClientSignal.AblationPoint>) : HardwareCommand()
        object StartAcquisition : HardwareCommand()
    }
}
