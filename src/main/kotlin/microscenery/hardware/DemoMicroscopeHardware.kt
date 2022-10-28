package microscenery.hardware

import graphics.scenery.utils.LazyLogger
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import graphics.scenery.volumes.Volume
import microscenery.nowMillis
import microscenery.signals.*
import org.joml.Vector2i
import org.joml.Vector3f
import org.joml.Vector3i
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import kotlin.concurrent.thread
import kotlin.math.roundToInt

/**
 * Demo Hardware that works on generated by [Volume.generateProceduralVolume]
 */
class DemoMicroscopeHardware(
    stagePosition: Vector3f = Vector3f(),
    var timeBetweenUpdatesMilli: Int = 200
) : MicroscopeHardwareAgent() {
    protected val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

    val side = 200
    val stageContent: ByteBuffer

    override val output: BlockingQueue<MicroscopeSignal> = ArrayBlockingQueue(10)

    var idCounter = 0
    var liveThread: Thread? = null
    var currentStack: Stack? = null

    init {
        stageContent = Volume.generateProceduralVolume(size = side.toLong(), radius = 190f, use16bit = false)

        hardwareDimensions = HardwareDimensions(
            stageMin = Vector3f(0f),
            stageMax = Vector3f(side.toFloat()),
            imageSize = Vector2i(50, 50),
            vertexSize = Vector3f(1f, 1f, 2f),
            numericType = NumericType.INT8
        )
        status = MicroscopeStatus(
            ServerState.MANUAL,
            stagePosition,
            false
        )

        //no need to start the agent
    }


    override var stagePosition = stagePosition
        set(target) {
            val safeTarget = coercePositionTarget(target)
            field = safeTarget
            status = status.copy(stagePosition = safeTarget)
        }

    override fun snapSlice() {
        val imgX = hardwareDimensions.imageSize.x
        val imgY = hardwareDimensions.imageSize.y
        val sliceBuffer = MemoryUtil.memAlloc(imgX * imgY)

        val fullSliceSize = side * side
        val zOffset = fullSliceSize * stagePosition.z.toInt()
        for (y in 0 until imgY) {
            val yOffset = side * (stagePosition.y.toInt() + y)
            val xOffset = stagePosition.x.toInt()
            val offset = zOffset + yOffset + xOffset
            stageContent.limit((offset + imgX).coerceIn(0, stageContent.capacity()))
            stageContent.position(offset.coerceIn(0, stageContent.capacity()))
            sliceBuffer.put(stageContent)
        }

        sliceBuffer.clear()

        val signal = Slice(
            idCounter++,
            System.currentTimeMillis(),
            stagePosition,
            sliceBuffer.capacity(),
            currentStack?.Id,
            sliceBuffer
        )
        output.put(signal)
    }

    override fun live(isLive: Boolean) {
        when {
            isLive && status.state == ServerState.MANUAL && liveThread == null -> {
                liveThread = thread(isDaemon = true) {
                    while (!Thread.currentThread().isInterrupted) {
                        snapSlice()
                        Thread.sleep(timeBetweenUpdatesMilli.toLong())
                    }
                }
                status = status.copy(state = ServerState.LIVE)
            }
            !isLive -> {
                liveThread?.interrupt()
                status = status.copy(state = ServerState.MANUAL)
            }
        }
    }

    override fun shutdown() {
        live(false)
    }

    override fun acquireStack(meta: ClientSignal.AcquireStack) {
        if (status.state != ServerState.MANUAL) {
            logger.warn("Ignoring Stack command because microscope is busy.")
        }

        status = status.copy(state = ServerState.STACK)
        thread {

            val start = coercePositionTarget(meta.startPosition)
            val end = coercePositionTarget(meta.endPosition)
            val dist = end - start
            val steps = (dist.length() / meta.stepSize).roundToInt()
            val step = dist * (1f / steps)

            currentStack = Stack(idCounter++, false, start, Vector3i(hardwareDimensions.imageSize, steps), nowMillis())
            output.put(currentStack!!)

            for (i in 0 until steps) {
                stagePosition = start + (step * i.toFloat())
                snapSlice()
            }

            currentStack = null
            status = status.copy(state = ServerState.MANUAL)
        }
    }

    override fun onLoop() {
        throw NotImplementedError("demo hardware has no active agent")
    }

    override fun moveStage(target: Vector3f) {
        throw NotImplementedError("demo does not use MicroscopeAgents stage handling")
    }

    private fun coercePositionTarget(target: Vector3f): Vector3f {
        val safeTarget = Vector3f()
        for (i in 0..2) safeTarget.setComponent(
            i,
            target[i].coerceIn(hardwareDimensions.stageMin[i], hardwareDimensions.stageMax[i])
        )
        if (safeTarget != target) {
            logger.warn("Had to coerce stage parameters! From $target to ${safeTarget}")
        }
        return safeTarget
    }
}
