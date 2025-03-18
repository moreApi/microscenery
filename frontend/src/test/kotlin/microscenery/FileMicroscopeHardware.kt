package microscenery

import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.xy
import graphics.scenery.utils.lazyLogger
import ij.IJ
import ij.ImagePlus
import microscenery.hardware.MicroscopeHardwareAgent
import microscenery.signals.*
import microscenery.signals.Stack
import net.imglib2.img.Img
import net.imglib2.img.display.imagej.ImageJFunctions
import net.imglib2.type.numeric.integer.UnsignedShortType
import org.joml.Vector2i
import org.joml.Vector3f
import org.joml.Vector3i
import org.lwjgl.system.MemoryUtil
import java.util.*
import java.util.concurrent.*
import kotlin.concurrent.thread

/**
 * Dummy microscope hardware implementation that reads a volume from file.
 */
class FileMicroscopeHardware(
    file: String,
    stagePosition: Vector3f = Vector3f(),
    var zPerXY: Float = 1f
) : MicroscopeHardwareAgent() {
    protected val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    lateinit var imp: ImagePlus
    lateinit var img: Img<UnsignedShortType>

    val dimensions: Vector3i
        get() {
            return Vector3i(img.dimension(0).toInt(), img.dimension(1).toInt(), img.dimension(2).toInt())
        }

    override val output: BlockingQueue<MicroscopeSignal> = ArrayBlockingQueue(10)

    var idCounter = 0
    var liveThread: Thread? = null
    var currentStack: Stack? = null
    var stackSliceCounter: Int = 0

    override var stagePosition = stagePosition
        set(target) {
            val safeTarget = hardwareDimensions.coercePosition(target, logger)
            field = safeTarget
            status = status.copy(stagePosition = safeTarget)
        }


    fun loadImg(file:String){
        imp = IJ.openImage(file)
        img = ImageJFunctions.wrap(imp)

        val im = ImageMeta(
            imageSize = Vector2i(dimensions.x, dimensions.y),
            vertexDiameter = 1f,
            numericType = NumericType.INT16
        )
        hardwareDimensions = HardwareDimensions(
            stageMin = Vector3f(0f),
            stageMax = Vector3f(0f, 0f, dimensions.z.toFloat()*zPerXY),
            im
        )
        imageMeta = im
        status = MicroscopeStatus(
            ServerState.MANUAL,
            stagePosition,
            false
        )
    }

    init {
        loadImg(file)

        //no need to start the agent
    }

    override fun snapSlice() {
        val imgX = hardwareDimensions.imageSize.x
        val imgY = hardwareDimensions.imageSize.y
        val sliceBuffer = MemoryUtil.memAlloc(imgX * imgY * 2)
        val shortBuffer = sliceBuffer.asShortBuffer()

        val z = stagePosition.z.toInt()

        val ra = img.randomAccess()

        for (y in 0 until imgY) {
            for (x in 0 until imgX) {
                shortBuffer.put(ra.setPositionAndGet(x, y, z).get().toShort())
            }
        }

        sliceBuffer.rewind()

        val signal = Slice(
            idCounter++,
            System.currentTimeMillis(),
            Vector3f(stagePosition.xy(),stagePosition.z * zPerXY),
            sliceBuffer.capacity(),
            currentStack?.let { it.Id to stackSliceCounter },
            imageMeta,
            sliceBuffer
        )
        output.put(MicroscopeSlice(signal))
    }

    override fun goLive() {
        logger.warn("File Microscope does not support live mode")
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

            val start = Vector3f(0f)
            val end = Vector3f(0f, 0f, dimensions.z * zPerXY)
            val steps = dimensions.z

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
                stagePosition = start + Vector3f(0f, 0f, i.toFloat())
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
        logger.warn("File Microscope does not support stage handling")
    }

    override fun startAcquisition() {
        snapSlice()
    }
}
