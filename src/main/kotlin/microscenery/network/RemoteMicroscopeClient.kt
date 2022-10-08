package microscenery.network

import graphics.scenery.utils.LazyLogger
import microscenery.Agent
import microscenery.MicroscenerySettings
import microscenery.hardware.MicroscopeHardware
import microscenery.signals.*
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


    override val output: BlockingQueue<MicroscopeSignal>
        get() = TODO("Not yet implemented")


    private val requestedSlices = ConcurrentHashMap<Int, Slice>()

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


     var stagePosition: Vector3f
        get() = TODO("Not yet implemented")
        set(value) {controlConnection.sendSignal(ClientSignal.MoveStage(value))}

    override fun hardwareDimensions(): HardwareDimensions {
        TODO("Not yet implemented")
    }

     fun snapSlice() {
        controlConnection.sendSignal(ClientSignal.SnapImage)
    }

    /**
     * Executed by the network thread of [ControlSignalsClient]
     */
    private fun processServerSignal(signal: RemoteMicroscopeSignal){

        when (signal){
            is ActualMicroscopeSignal -> {
                val microscopesignal = signal.signal
                when (microscopesignal) {
                    is HardwareDimensions -> output.put(microscopesignal)
                    is MicroscopeStatus -> {
                        if (microscopesignal.state == ServerState.SHUTTING_DOWN)
                            this.close()
                    }
                    is Slice -> {
                        if (dataConnection.requestSlice(microscopesignal.Id, microscopesignal.size)) {
                            // save signal for eventual data receiving
                            requestedSlices[microscopesignal.Id] = microscopesignal
                        }
                    }
                    is Stack -> TODO()
                }
            }
            is RemoteMicroscopeStatus -> TODO()
        }
    }

    override fun snapSlice(target: Vector3f) {
        TODO("Not yet implemented")
    }

    @Suppress("unused")
    override fun shutdown() {
        logger.info("Got Stop Command")
        controlConnection.sendSignal(ClientSignal.Shutdown)
        close()
    }

    override fun status(): MicroscopeStatus {
        TODO("Not yet implemented")
    }
}