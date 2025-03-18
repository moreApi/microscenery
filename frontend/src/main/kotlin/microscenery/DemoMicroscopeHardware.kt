package microscenery

import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import graphics.scenery.utils.lazyLogger
import microscenery.hardware.MicroscopeHardwareAgent
import microscenery.signals.*
import microscenery.signals.Stack
import microscenery.simulation.Procedural
import org.joml.Vector2i
import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Semaphore
import kotlin.concurrent.thread
import kotlin.math.roundToInt

/**
 * Demo Hardware that returns generated data like [Volume.generateProceduralVolume]
 *
 * @param binning also scales the stage area by the binning amount.
 */
class DemoMicroscopeHardware(
    stagePosition: Vector3f = Vector3f(),
    var timeBetweenUpdatesMilli: Int = 200,
    val size: Vector3f = Vector3f(300f),
    val binning: Int = 1
) : MicroscopeHardwareAgent() {
    protected val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    override val output: BlockingQueue<MicroscopeSignal> = ArrayBlockingQueue(10)

    val procedural = Procedural()

    var idCounter = 0
    var liveThread: Thread? = null
    var currentStack: Stack? = null
    var stackSliceCounter: Int = 0

    init {
        val im = ImageMeta(
            Vector2i(50 / binning, 50 / binning),
            vertexDiameter = 1f * binning,
            numericType = NumericType.INT8
        )
        hardwareDimensions = HardwareDimensions(
            stageMin = Vector3f(0f),
            stageMax = size,
            imageMeta = im
        )
        imageMeta = im
        status = MicroscopeStatus(
            ServerState.MANUAL,
            stagePosition,
            false
        )

        //no need to start the agent
    }

    override var stagePosition = stagePosition
        set(target) {
            val safeTarget = hardwareDimensions.coercePosition(target, logger)
            field = safeTarget
            status = status.copy(stagePosition = safeTarget)
        }

    override fun snapSlice() {
        val sliceBuffer = when (binning) {
            1 -> {
                procedural.slice(stagePosition, hardwareDimensions.imageSize)
            }

            else -> {
                val imgX = hardwareDimensions.imageSize.x
                val imgY = hardwareDimensions.imageSize.y
                val sliceBuffer = MemoryUtil.memAlloc(imgX * imgY)

                val dataX = imgX * binning
                val dataY = imgY * binning
                val dataBuffer = procedural.slice(stagePosition, Vector2i(dataX, dataY))

                for (imgYPos in 0 until imgY) for (imgXPos in 0 until imgX) {
                    var collector = 0
                    for (binY in 0 until binning) for (binX in 0 until binning) {

                        val dataSliceXPos = imgXPos * binning + binX
                        val dataSliceYPos = imgYPos * binning + binY

                        val yOffset = dataY * (stagePosition.y.toInt() + dataSliceYPos)
                        val xOffset = stagePosition.x.toInt() + dataSliceXPos
                        val offset = yOffset + xOffset
                        collector += dataBuffer.get(offset)
                    }
                    sliceBuffer.put((collector / binning).toByte())
                }
                MemoryUtil.memFree(dataBuffer)
                sliceBuffer
            }
        }

        val signal = Slice(
            idCounter++,
            System.currentTimeMillis(),
            stagePosition,
            sliceBuffer.capacity(),
            currentStack?.let { it.Id to stackSliceCounter },
            imageMeta,
            sliceBuffer
        )
        output.put(MicroscopeSlice(signal))
    }

    override fun goLive() {
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

    override fun sync(): Semaphore {
        return Semaphore(1) // It's already free. Come get it!
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

    override fun acquireStack(meta: MicroscopeControlSignal.AcquireStack) {
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
                start,
                end,
                steps,
                nowMillis(),
                imageMeta
            )
            output.put(MicroscopeStack(currentStack!!))

            for (i in 0 until steps) {
                stagePosition = start + (step * i.toFloat())
                stackSliceCounter = i
                snapSlice()
            }

            currentStack = null
            status = status.copy(state = ServerState.MANUAL)
        }
    }

    override fun ablatePoints(signal: MicroscopeControlSignal.AblationPoints) {
        for (p in signal.points)
            logger.info("Ablating $p")
        output.put(AblationResults(signal.points.size * 50, (1..signal.points.size).map { Random().nextInt(20) + 40 }))
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
}
