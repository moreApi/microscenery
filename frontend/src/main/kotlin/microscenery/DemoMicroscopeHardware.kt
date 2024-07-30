package microscenery

import graphics.scenery.numerics.OpenSimplexNoise
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
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

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

    var idCounter = 0
    var liveThread: Thread? = null
    var currentStack: Stack? = null
    var stackSliceCounter: Int = 0

    init {

        hardwareDimensions = HardwareDimensions(
            stageMin = Vector3f(0f),
            stageMax = size,
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
        val sliceBuffer = when (binning) {
            1 -> {
                genSlice(stagePosition,hardwareDimensions.imageSize)
            }
            else -> {
                val imgX = hardwareDimensions.imageSize.x
                val imgY = hardwareDimensions.imageSize.y
                val sliceBuffer = MemoryUtil.memAlloc(imgX * imgY)

                val dataX = imgX * binning
                val dataY = imgY * binning
                val dataBuffer = genSlice(stagePosition, Vector2i(dataX,dataY))

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

    companion object{
        class GenerationParams(val size: Int = 200,
                               val seed: Long = 1337L,
                               val use16bit: Boolean = false,
                               val radius: Float = size * 0.95f)

        /**
         * Algorithm copied from [Volume.generateProceduralVolume]
         */
        fun genSlice(pos: Vector3f, imgSize: Vector2i, params: GenerationParams = GenerationParams()): ByteBuffer{
            val size = params.size
            val seed = params.seed
            val use16bit = params.use16bit
            val radius = params.radius

            val f = 3.0f / size
            val center = size / 2.0f + 0.5f
            val noise = OpenSimplexNoise(seed)
            val (range, bytesPerVoxel) = if(use16bit) {
                65535 to 2
            } else {
                255 to 1
            }
            val byteSize = (imgSize.x*imgSize.y*bytesPerVoxel).toInt()

            val buffer = MemoryUtil.memAlloc(byteSize)
            val shortBufferView = buffer.asShortBuffer()

            for (y in pos.y.toInt() until pos.y.toInt()+imgSize.y)
                for (x in pos.x.toInt() until pos.x.toInt()+imgSize.x) {
                    val z = pos.z

                    val dx = center - x
                    val dy = center - y
                    val dz = center - z

                    val offset = abs(noise.random3D((x) * f, (y) * f, (z) * f))
                    val d = sqrt(dx * dx + dy * dy + dz * dz) / size

                    val result = if(radius > Math.ulp(1.0f)) {
                        if(d - offset < radius) { ((d-offset)*range).toInt().toShort() } else { 0 }
                    } else {
                        ((d - offset) * range).toInt().toShort()
                    }

                    if(use16bit) {
                        shortBufferView.put(result)
                    } else {
                        buffer.put(result.toByte())
                    }
                }
            buffer.clear()
            return buffer
        }
    }
}
