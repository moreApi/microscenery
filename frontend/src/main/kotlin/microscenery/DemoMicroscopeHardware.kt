package microscenery

import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import graphics.scenery.utils.lazyLogger
import graphics.scenery.volumes.Volume
import microscenery.hardware.MicroscopeHardwareAgent
import microscenery.signals.*
import microscenery.signals.Stack
import org.joml.Vector2i
import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import kotlin.concurrent.thread
import kotlin.math.roundToInt

/**
 * Demo Hardware that works on generated by [Volume.generateProceduralVolume]
 *
 * @param binning also scales the stage area by the binning amount.
 */
class DemoMicroscopeHardware(
    stagePosition: Vector3f = Vector3f(),
    var timeBetweenUpdatesMilli: Int = 200,
    val dataSide: Int = 200,
    val binning: Int = 1
) : MicroscopeHardwareAgent() {
    protected val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))


    val stageContent: ByteBuffer

    override val output: BlockingQueue<MicroscopeSignal> = ArrayBlockingQueue(10)

    var idCounter = 0
    var liveThread: Thread? = null
    var currentStack: Stack? = null
    var stackSliceCounter: Int = 0

    init {
        stageContent =
            Volume.generateProceduralVolume(size = dataSide.toLong(), radius = dataSide * 0.95f, use16bit = false)

        hardwareDimensions = HardwareDimensions(
            stageMin = Vector3f(0f),
            stageMax = Vector3f(dataSide.toFloat()),
            imageSize = Vector2i(50 / binning, 50 / binning),
            vertexDiameter = 1f * binning,
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
            val safeTarget = hardwareDimensions.coercePosition(target, logger)
            field = safeTarget
            status = status.copy(stagePosition = safeTarget)
        }

    override fun snapSlice() {
        val imgX = hardwareDimensions.imageSize.x
        val imgY = hardwareDimensions.imageSize.y
        val sliceBuffer = MemoryUtil.memAlloc(imgX * imgY)

        val fullSliceSize = dataSide * dataSide
        val zOffset = fullSliceSize * stagePosition.z.toInt()

        when (binning) {
            1 -> {
                for (y in 0 until imgY) {
                    val yOffset = dataSide * (stagePosition.y.toInt() + y)
                    val xOffset = stagePosition.x.toInt()
                    val offset = zOffset + yOffset + xOffset
                    stageContent.limit((offset + imgX).coerceIn(0, stageContent.capacity()))
                    stageContent.position(offset.coerceIn(0, stageContent.capacity()))
                    sliceBuffer.put(stageContent)
                }
            }
            else -> {
                for (imgYPos in 0 until imgY) for (imgXPos in 0 until imgX) {
                    var collector = 0
                    for (binY in 0 until binning) for (binX in 0 until binning) {

                        val dataSliceXPos = imgXPos * binning + binX
                        val dataSliceYPos = imgYPos * binning + binY

                        val yOffset = dataSide * (stagePosition.y.toInt() + dataSliceYPos)
                        val xOffset = stagePosition.x.toInt() + dataSliceXPos
                        val offset = zOffset + yOffset + xOffset
                        collector += stageContent.get(offset)
                    }
                    sliceBuffer.put((collector / binning).toByte())
                }
            }
        }


        sliceBuffer.rewind()

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
}
