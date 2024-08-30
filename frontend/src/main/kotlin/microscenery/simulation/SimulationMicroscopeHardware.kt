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
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**

 */
class SimulationMicroscopeHardware(
    val msHub: MicrosceneryHub,
    stagePosition: Vector3f = Vector3f(),
    imageSize: Vector2i = Vector2i(250),
    stageSize: Vector3f = Vector3f(300f)
) : MicroscopeHardwareAgent() {
    protected val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    override val output: BlockingQueue<MicroscopeSignal> = ArrayBlockingQueue(10)

    val focalPlane = Plane(Vector3f(Vector2f(imageSize),1f,))

    var idCounter = 0
    var liveThread: Thread? = null
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

        //no need to start the agent
    }

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

}
