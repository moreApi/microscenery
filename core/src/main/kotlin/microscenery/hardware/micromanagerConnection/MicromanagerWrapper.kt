package microscenery.hardware.micromanagerConnection

import fromScenery.lazyLogger
import fromScenery.utils.extensions.minus
import fromScenery.utils.extensions.plus
import fromScenery.utils.extensions.times
import fromScenery.utils.extensions.xy
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
import kotlin.math.roundToInt
import kotlin.system.measureNanoTime

/**
 * Wrapper of a [MMCoreConnector] to fit the MicroscopeHardware interface.
 * Queues incoming commands
 *
 * @param disableStagePosUpdates if true the position of the stage is only updated on move commands. Makes status updates more deterministic for tests.
 */
class MicromanagerWrapper(
    private val mmCoreConnector: MMCoreConnector,
    private val mmStudioConnector: MMStudioConnector = DummyMMStudioConnector(),
    var timeBetweenUpdates: Int = MicroscenerySettings.get("MMConnection.TimeBetweenStackAcquisition", 1000),
    val disableStagePosUpdates: Boolean = false
) : MicroscopeHardwareAgent() {
    protected val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private val hardwareCommandsQueue = ArrayBlockingQueue<HardwareCommand>(5000)

    // this lock is only relevant for self replicating commands e.g. snapSlice(live=true)
    private val stopLock = Any()

    private var ablationThread: Thread? = null

    // for Slices and stacks
    private var idCounter = 0
    var lastSnap = 0L
    var vertexDiameter = MicroscenerySettings.get("MMConnection.vertexDiameter", mmCoreConnector.pixelSizeUm)
        set(value) {
            field = value
            updateHardwareDimensions()
        }

    init {
        MicroscenerySettings.setVector3fIfUnset("Stage.min", mmCoreConnector.stagePosition)
        MicroscenerySettings.setVector3fIfUnset("Stage.max", mmCoreConnector.stagePosition)
        MicroscenerySettings.setIfUnset("MMConnection.useStudioAPI",mmStudioConnector !is DummyMMStudioConnector )


        updateHardwareDimensions()

        if (hardwareDimensions.coercePosition(mmCoreConnector.stagePosition, null) != mmCoreConnector.stagePosition){
            val msg = "Stage ${mmCoreConnector.stagePosition.toReadableString()} not in allowed area " +
                    "from ${MicroscenerySettings.getVector3("Stage.min")?.toReadableString()}" +
                    "to ${MicroscenerySettings.getVector3("Stage.max")?.toReadableString()}. Aborting!" +
                    "Adjust stage position or properties file."
            logger.error(msg)
            throw IllegalStateException(msg)
        }

        startAgent()
        status = status.copy(stagePosition = mmCoreConnector.stagePosition, state = ServerState.MANUAL)
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

    /**
     * Sets the stage position to [pos] in memory but does now issue commands to the hardware.
     * Used for updates from the hardware cause by manual movement by the user.
     */
    @Suppress("unused")
    fun updateStagePositionNoMovement(pos: Vector3f){
        if (status.state != ServerState.STARTUP && !disableStagePosUpdates) {
            status = status.copy(stagePosition = pos)
        }
    }

    /**
     * Send data as slice.
     * Used for data acquired by use of the traditional micromanager interface.
     */
    @Suppress("unused")
    fun externalSnap(position: Vector3f, data: ByteBuffer){
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
                if(MicroscenerySettings.get("MMConnection.useStudioAPI",false)) {
                    mmStudioConnector.live(false)
                }
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
        if(hwCommand.live && MicroscenerySettings.get("MMConnection.useStudioAPI",false)) {
            mmStudioConnector.live(true)
            return
        }

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

        if (MicroscenerySettings.get("MMConnection.useStudioAPI",false)) {
            //TODO handle stack slices
            mmStudioConnector.snap()
        } else {
            val buf = MemoryUtil.memAlloc(hardwareDimensions.byteSize)
            (buf as Buffer).clear() // this cast has to be done to be compatible with JDK 8
            try {
                mmCoreConnector.snapSlice(buf.asShortBuffer())
            } catch (t: Throwable) {
                logger.warn("Failed snap command", t)
                return
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
            addToCommandQueueIfNotStopped(HardwareCommand.GenerateStackCommands(
                signal = meta.copy(id = currentStack.Id)))
        }
    }

    private fun executeAblatePoints(hwCommand: HardwareCommand.AblatePoints) {
        //this is a thread so it can be interrupted asynchronously
        var totalTime = 0
        val perPointTime = mutableListOf<Int>()
        val debug = MicroscenerySettings.get("debug",false)
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
                            if (debug){
                                executeMoveStage(point.position,true)
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
        val overXYPrecision = MicroscenerySettings.getOrNull<Float>("Stage.precisionXY")?.let {
            !stagePosition.xy().equals(target.xy(), it)
        } ?: true
        val overZPrecision = MicroscenerySettings.getOrNull<Float>("Stage.precisionZ")?.let { precision ->
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


        if (MicroscenerySettings.get("MMConnection.OriginMoveProtection", true)
            && target == Vector3f(0f)){//( target.x == 0f || target.y == 0f || target.z == 0f)){
            logger.warn("Ignoring stage move command because MMConnection.OriginMoveProtection is true")
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
