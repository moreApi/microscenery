package microscenery.network

import graphics.scenery.Hub
import graphics.scenery.Scene
import graphics.scenery.primitives.TextBoard
import graphics.scenery.utils.LazyLogger
import microscenery.MicroscenerySettings
import microscenery.MicroscopeHardware
import org.joml.Vector3f
import org.joml.Vector4f
import org.zeromq.ZContext
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 *
 */
class ControlledVolumeStreamClient(
    val scene: Scene,
    val hub: Hub,
    val storage: SliceStorage,
    basePort: Int = MicroscenerySettings.get("Network.basePort"),
    host: String = MicroscenerySettings.get("Network.host"),
    val zContext: ZContext = microscenery.zContext
) : MicroscopeHardware {
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private val controlConnection = ControlSignalsClient(zContext, basePort, host)
    private val dataConnection = BiggishDataClient(zContext,basePort +1, host)

    override var latestServerStatus: ServerSignal.ServerStatus? = null
    var lastAcquisitionSignal = 0L
    override val newSlice: BlockingQueue<Pair<ServerSignal.Slice, ByteBuffer>> = ArrayBlockingQueue(10)

    var running = true
    val thread: Thread

    private val requestedSlices = ConcurrentHashMap<Int,ServerSignal.Slice>()

    init {
        controlConnection.addListener (this::processServerSignal)
        thread = thread {
            while (running && Thread.interrupted()){
                val sliceParts = dataConnection.outputQueue.poll(200, TimeUnit.MILLISECONDS) ?: continue
                val meta = requestedSlices[sliceParts.id]

                if (meta == null){
                    logger.warn("Got data for slice ${sliceParts.id} but it was not requested.")
                    continue
                }
                if (sliceParts.size != meta.size) {
                    logger.error("Size mismatch for slice ${sliceParts.id} ${sliceParts.size} vs ${meta.size}")
                }

                val buffer = storage.newSlice(sliceParts.size)
                sliceParts.chunks.forEach{
                    buffer.put(it.value)
                }
                buffer.flip()
                newSlice.put(meta to buffer)
            }
        }
    }


    override fun moveStage(target: Vector3f) {
        controlConnection.sendSignal(ClientSignal.MoveStage(target))
    }

    override fun snapSlice() {
        controlConnection.sendSignal(ClientSignal.SnapImage)
    }

    /**
     * Executed by the network thread of [ControlSignalsClient]
     */
    private fun processServerSignal(signal: ServerSignal){
        when (signal){
            is ServerSignal.ServerStatus -> latestServerStatus = signal
            is ServerSignal.Slice -> {
                if (dataConnection.requestSlice(signal.Id,signal.size)){
                    // save signal for eventual data receiving
                    requestedSlices[signal.Id] = signal
                }
            }
            is ServerSignal.Stack -> TODO()
        }
    }

    @Suppress("unused")
    fun start() {
        logger.info("Got Start Command")
        //if (latestServerStatus?.state == ServerState.Paused) controlConnection.sendSignal(ClientSignal.StartImaging)
    }

    @Suppress("unused")
    fun snap() {
        logger.info("Got Snap Command")
        //if (latestServerStatus?.state == ServerState.Paused) controlConnection.sendSignal(ClientSignal.SnapStack)
    }

    @Suppress("unused")
    fun pause() {
        logger.info("Got Pause Command")
        //if (latestServerStatus?.state == ServerState.Imaging) controlConnection.sendSignal(ClientSignal.StopImaging)
    }

    @Suppress("unused")
    fun shutdown() {
        logger.info("Got Stop Command")
        controlConnection.sendSignal(ClientSignal.Shutdown)
    }


    fun lastAcquisitionTextBoard(): TextBoard {
        val lastUpdateBoard = TextBoard()
        lastUpdateBoard.text = "Last Acquisition Signal: never"
        lastUpdateBoard.transparent = 0
        lastUpdateBoard.fontColor = Vector4f(0.0f, 0.0f, 0.0f, 1.0f)
        lastUpdateBoard.backgroundColor = Vector4f(100f, 100f, 100f, 1.0f)
        lastUpdateBoard.update += {
            if (this.lastAcquisitionSignal != 0L) {
                val dif = System.currentTimeMillis() - this.lastAcquisitionSignal
                lastUpdateBoard.text = "Last Acquisition Signal: " + (dif / 100).toFloat() / 10 + "sec"
            }
        }
        return lastUpdateBoard
    }
}