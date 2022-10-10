package microscenery.network

import graphics.scenery.utils.LazyLogger
import microscenery.MicroscenerySettings
import microscenery.hardware.MicroscopeHardwareAgent
import microscenery.signals.*
import org.joml.Vector3f
import org.zeromq.ZContext
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
) : MicroscopeHardwareAgent() {
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private val controlConnection = ControlSignalsClient(zContext, basePort, host)
    private val dataConnection = BiggishDataClient(zContext,basePort +1, host)

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

    override fun snapSlice(target: Vector3f) {
        controlConnection.sendSignal(ClientSignal.MoveStage(target))
        controlConnection.sendSignal(ClientSignal.SnapImage)
    }

    /**
     * Executed by the network thread of [ControlSignalsClient]
     */
    private fun processServerSignal(signal: RemoteMicroscopeSignal){

        when (signal){
            is ActualMicroscopeSignal -> {
                when (val microscopeSignal = signal.signal) {
                    is HardwareDimensions -> output.put(microscopeSignal)
                    is MicroscopeStatus -> {
                        if (microscopeSignal.state == ServerState.SHUTTING_DOWN)
                            this.close()
                    }
                    is Slice -> {
                        if (dataConnection.requestSlice(microscopeSignal.Id, microscopeSignal.size)) {
                            // save signal for eventual data receiving
                            requestedSlices[microscopeSignal.Id] = microscopeSignal
                        }
                    }
                    is Stack -> TODO()
                }
            }
            is RemoteMicroscopeStatus -> TODO()
        }
    }

    @Suppress("unused")
    override fun shutdown() {
        logger.info("Got Stop Command")
        close()
    }

    override fun onClose() {
        controlConnection.sendSignal(ClientSignal.Shutdown)
    }
}