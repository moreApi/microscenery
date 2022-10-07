package microscenery.network

import graphics.scenery.utils.LazyLogger
import microscenery.Agent
import microscenery.MicroscenerySettings
import microscenery.hardware.MicroscopeHardware
import org.joml.Vector3f
import org.zeromq.ZContext
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 *
 */
class RemoteMicroscopeClient(
    val storage: SliceStorage,
    basePort: Int = MicroscenerySettings.get("Network.basePort"),
    host: String = MicroscenerySettings.get("Network.host"),
    val zContext: ZContext
) : Agent(), MicroscopeHardware {
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private val controlConnection = ControlSignalsClient(zContext, basePort, host)
    private val dataConnection = BiggishDataClient(zContext,basePort +1, host)

    override fun serverStatus(): ServerSignal.ServerStatus {
        TODO("Not yet implemented")
    }

    override val output: BlockingQueue<ServerSignal>
        get() = TODO("Not yet implemented")


    private val requestedSlices = ConcurrentHashMap<Int,ServerSignal.Slice>()

    init {
        controlConnection.addListener(this::processServerSignal)
        startAgent()
    }

    override fun onLoop() {
        val sliceParts = dataConnection.outputQueue.poll(200, TimeUnit.MILLISECONDS) ?: return
        val meta = requestedSlices[sliceParts.id]

        if (meta == null){
            logger.warn("Got data for slice ${sliceParts.id} but it was not requested.")
            return
        }
        if (sliceParts.size != meta.size) {
            logger.error("Size mismatch for slice ${sliceParts.id} ${sliceParts.size} vs ${meta.size}")
        }

        val buffer = storage.newSlice(sliceParts.size)
        sliceParts.chunks.forEach{
            buffer.put(it.value)
        }
        buffer.flip()
        //todo newSlice.put(meta to buffer)
    }


    override var stagePosition: Vector3f
        get() = TODO("Not yet implemented")
        set(value) {controlConnection.sendSignal(ClientSignal.MoveStage(value))}

    override fun hardwareDimensions(): HardwareDimensions {
        TODO("Not yet implemented")
    }

    override fun snapSlice() {
        controlConnection.sendSignal(ClientSignal.SnapImage)
    }

    /**
     * Executed by the network thread of [ControlSignalsClient]
     */
    private fun processServerSignal(signal: ServerSignal){
        when (signal){
            is ServerSignal.ServerStatus -> {
                // todo latestServerStatus = signal
                if ( signal.state == ServerState.SHUTTING_DOWN)
                    this.close()
            }
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
    override fun shutdown() {
        logger.info("Got Stop Command")
        controlConnection.sendSignal(ClientSignal.Shutdown)
        close()
    }
}