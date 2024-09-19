package microscenery.simulation

import graphics.scenery.Scene
import graphics.scenery.attribute.material.Material
import graphics.scenery.numerics.OpenSimplexNoise
import graphics.scenery.primitives.Plane
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import graphics.scenery.utils.lazyLogger
import graphics.scenery.volumes.Volume
import microscenery.MicrosceneryHub
import microscenery.discover
import microscenery.hardware.MicroscopeHardwareAgent
import microscenery.hardware.micromanagerConnection.MicromanagerWrapper
import microscenery.nowMillis
import microscenery.signals.*
import microscenery.signals.Stack
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**

 */
class SimulationMicroscopeHardware(
    val msHub: MicrosceneryHub,
    stagePosition: Vector3f = Vector3f(),
    imageSize: Vector2i = Vector2i(250),
    stageSize: Vector3f = Vector3f(300f),
    val maxIntensity: Short = Short.MAX_VALUE
) : MicroscopeHardwareAgent() {
    protected val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    override val output: BlockingQueue<MicroscopeSignal> = ArrayBlockingQueue(10)

    val focalPlane = Plane(Vector3f(Vector2f(imageSize),1f,))

    private val hardwareCommandsQueue = ArrayBlockingQueue<HardwareCommand>(5000)

    // this lock is only relevant for self replicating commands e.g. snapSlice(live=true)
    private val stopLock = Any()

    // for Slices and stacks
    var idCounter = 0
    var lastSnap = 0L
    var currentStack: Stack? = null
    var stackSliceCounter: Int = 0

    init {

        focalPlane.material().cullingMode = Material.CullingMode.FrontAndBack

        hardwareDimensions = HardwareDimensions(
            stageMin = stageSize * -0.5f,
            stageMax = stageSize * 0.5f,
            imageSize = imageSize,
            vertexDiameter = 1f,
            numericType = NumericType.INT16
        )
        status = MicroscopeStatus(
            ServerState.MANUAL,
            stagePosition,
            false
        )

        startAgent()
    }
/*
    override var stagePosition = stagePosition
        set(target) {
            val safeTarget = hardwareDimensions.coercePosition(target, logger)
            field = safeTarget
            focalPlane.spatial().position = safeTarget
            status = status.copy(stagePosition = safeTarget)
        }

    override fun snapSlice() {
        val imgX = hardwareDimensions.imageSize.x
        val imgY = hardwareDimensions.imageSize.y
        val sliceBuffer = MemoryUtil.memAlloc(imgX * imgY * 2)
        val shortBuffer = sliceBuffer.asShortBuffer()

        val microDummys = getMicroDummysInFocus()

        for (y in 0 until imgY) {
            for (x in 0 until imgX) {
                shortBuffer.put(
                    microDummys
                        .map { it.intensity(Vector3f(x.toFloat()-imgX/2,y.toFloat()-imgY/2,0f)+ stagePosition) }
                        .sum()
                        .toShort()
                        .coerceAtMost(maxIntensity)
                )
            }
        }


        val signal = Slice(
            idCounter++,
            System.currentTimeMillis(),
            stagePosition,
            sliceBuffer.capacity(),
            currentStack?.let { it.Id to stackSliceCounter },
            sliceBuffer
        )
        output.put(signal)
    }

    private fun getMicroDummysInFocus(): List<Simulatable> {
        val stageSpaceManager = msHub.getAttribute(StageSpaceManager::class.java)
        if (focalPlane.parent == null) stageSpaceManager.stageRoot.addChild(focalPlane)

        return focalPlane.getScene()?.discover { it.getAttributeOrNull(Simulatable::class.java) != null }
            ?.filter {
                it.boundingBox?.intersects(focalPlane.boundingBox!!) ?: false
            }?.map {
                it.getAttribute(Simulatable::class.java)
            } ?: emptyList()
    }

    override fun goLive() {
        val timeBetweenUpdatesMilli = 200
        if (
            status.state == ServerState.MANUAL && liveThread == null) {
            liveThread = thread(isDaemon = true) {
                while (!Thread.currentThread().isInterrupted) {
                    snapSlice()
                    Thread.sleep(timeBetweenUpdatesMilli.toLong())
                }
            }
            status = status.copy(state = ServerState.LIVE)
        } else {
            logger.warn("Microscope not Manual (is ${status.state}) -> not going live")
        }
    }

    override fun stop() {

        if (status.state == ServerState.LIVE) {
            liveThread?.interrupt()
            liveThread = null
            status = status.copy(state = ServerState.MANUAL)
        }
    }

    override fun shutdown() {
        stop()
    }

    override fun acquireStack(meta: ClientSignal.AcquireStack) {
        if (status.state != ServerState.MANUAL) {
            logger.warn("Ignoring Stack command because microscope is busy.")
        }

        status = status.copy(state = ServerState.STACK)
        thread {

            val start = meta.startPosition
            val end = meta.endPosition
            val dist = end - start
            val steps = (dist.length() / meta.stepSize).roundToInt()
            val step = dist * (1f / steps)

            currentStack = Stack(
                idCounter++,
                false,
                start,
                end,
                steps,
                nowMillis()
            )
            output.put(currentStack!!)

            for (i in 0 until steps) {
                stagePosition = start + (step * i.toFloat())
                stackSliceCounter = i
                snapSlice()
            }

            currentStack = null
            status = status.copy(state = ServerState.MANUAL)
        }
    }

    override fun ablatePoints(signal: ClientSignal.AblationPoints) {
        for (p in signal.points)
            logger.info("Ablating $p")
        output.put(AblationResults(signal.points.size*50,(1..signal.points.size).map { Random().nextInt(20)+40 }))
    }

    override fun onLoop() {
        throw NotImplementedError("demo hardware has no active agent")
    }

    override fun moveStage(target: Vector3f) {
        throw NotImplementedError("demo does not use MicroscopeAgents stage handling")
    }

    override fun startAcquisition() {
        snapSlice()
    }

*/
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

    override fun startAcquisition() {
    }

    override fun deviceSpecificCommands(data: ByteArray) {
    }


    //############################## end of called from external threads ##############################

    override fun onLoop() {
        when (val hwCommand = hardwareCommandsQueue.poll(200, MILLISECONDS)) {
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
                status = status.copy(state = ServerState.MANUAL)
            }
            HardwareCommand.Shutdown -> TODO()
        }
    }

    private fun executeSnapImage(hwCommand: HardwareCommand.SnapImage) {
        if (hwCommand.live) {
            when (status.state) {
                ServerState.LIVE -> {}
                ServerState.MANUAL -> status = status.copy(state = ServerState.LIVE)
                else -> {
                    stop()
                    logger.error("Want to go live but server is ${status.state}. Stopping.")
                    return
                }
            }

            hardwareCommandsQueue.add(HardwareCommand.SnapImage(true))

            if (System.currentTimeMillis() - lastSnap  < 500){
                return
            }
        }

        val imgX = hardwareDimensions.imageSize.x
        val imgY = hardwareDimensions.imageSize.y
        val sliceBuffer = MemoryUtil.memAlloc(imgX * imgY * 2)
        val shortBuffer = sliceBuffer.asShortBuffer()

        val microDummys = getMicroDummysInFocus()

        for (y in 0 until imgY) {
            for (x in 0 until imgX) {
                shortBuffer.put(
                    microDummys
                        .map { it.intensity(Vector3f(x.toFloat()-imgX/2,y.toFloat()-imgY/2,0f)+ stagePosition) }
                        .sum()
                        .toShort()
                        .coerceAtMost(maxIntensity)
                )
            }
        }


        val signal = Slice(
            idCounter++,
            System.currentTimeMillis(),
            stagePosition,
            sliceBuffer.capacity(),
            hwCommand.stackIdAndSliceIndex,
            sliceBuffer
        )
        Thread.sleep(100)
        output.put(signal)
    }

    private fun getMicroDummysInFocus(): List<Simulatable> {
        val stageSpaceManager = msHub.getAttribute(StageSpaceManager::class.java)
        if (focalPlane.parent == null) stageSpaceManager.stageRoot.addChild(focalPlane)

        return focalPlane.getScene()?.discover { it.getAttributeOrNull(Simulatable::class.java) != null }
            ?.filter {
                it.boundingBox?.intersects(focalPlane.boundingBox!!) ?: false
            }?.map {
                it.getAttribute(Simulatable::class.java)
            } ?: emptyList()
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


    private fun executeMoveStage(target: Vector3f, wait: Boolean) {
        // skip if next command is also move
        if (hardwareCommandsQueue.peek() is HardwareCommand.MoveStage) return


        val safeTarget = hardwareDimensions.coercePosition(target, logger)
        focalPlane.spatial().position = safeTarget
        status = status.copy(stagePosition = safeTarget)
    }


    private fun addToCommandQueueIfNotStopped(vararg commands: HardwareCommand) {
        synchronized(stopLock) {
            if (hardwareCommandsQueue.peek() !is HardwareCommand.Stop) {
                commands.forEach { hardwareCommandsQueue.add(it) }
            }
        }
    }

    private sealed class HardwareCommand {
        val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

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
