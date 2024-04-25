package microscenery.network

import fromScenery.lazyLogger
import microscenery.MicroscenerySettings
import microscenery.hardware.MicroscopeHardwareAgent
import microscenery.signals.*
import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil
import org.zeromq.ZContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 *
 */
class RemoteMicroscopeClient(
    basePort: Int = MicroscenerySettings.get("Network.basePort",4000),
    host: String = MicroscenerySettings.get("Network.host","localhost"),
    val zContext: ZContext
) : MicroscopeHardwareAgent() {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private val controlConnection = ControlSignalsClient(zContext, basePort, host, listOf(this::processServerSignal))
    private val dataConnection = BiggishDataClient(zContext, basePort + 1, host)

    private val requestedSlices = ConcurrentHashMap<Int, Slice>()

    init {
        startAgent()
    }

    override fun onLoop() {
        val sliceParts = dataConnection.outputQueue.poll(200, TimeUnit.MILLISECONDS) ?: return
        val meta = requestedSlices[sliceParts.id]

        if (meta == null) {
            logger.warn("Got data for slice ${sliceParts.id} but it was not requested.")
            return
        }
        if (sliceParts.size != meta.size) {
            logger.error("Size mismatch for slice ${sliceParts.id} ${sliceParts.size} vs ${meta.size}")
        }

        val buffer = MemoryUtil.memAlloc(sliceParts.size)
        sliceParts.chunks.forEach {
            buffer.put(it.value)
        }
        buffer.flip()
        output.put(meta.copy(data = buffer))
    }

    override fun snapSlice() {
        controlConnection.sendSignal(ClientSignal.SnapImage)
    }

    override fun moveStage(target: Vector3f) {
        controlConnection.sendSignal(ClientSignal.MoveStage(target))
    }

    override fun acquireStack(meta: ClientSignal.AcquireStack) {
        controlConnection.sendSignal(meta)
    }

    override fun ablatePoints(signal: ClientSignal.AblationPoints) {
        controlConnection.sendSignal(signal)
    }

    override fun startAcquisition() {
        controlConnection.sendSignal(ClientSignal.StartAcquisition)
    }

    override fun goLive() {
        controlConnection.sendSignal(ClientSignal.Live)
    }

    override fun stop() {
        controlConnection.sendSignal(ClientSignal.Stop)
    }

    /**
     * Executed by the network thread of [ControlSignalsClient]
     */
    private fun processServerSignal(signal: RemoteMicroscopeSignal) {

        when (signal) {
            is ActualMicroscopeSignal -> {
                when (val microscopeSignal = signal.signal) {
                    is HardwareDimensions -> {
                        hardwareDimensions = microscopeSignal
                    }
                    is MicroscopeStatus -> {
                        if (microscopeSignal.state == ServerState.SHUTTING_DOWN) this.close()
                        status = microscopeSignal
                    }
                    is Slice -> {
                        if (dataConnection.requestSlice(microscopeSignal.Id, microscopeSignal.size)) {
                            // save signal for eventual data receiving
                            requestedSlices[microscopeSignal.Id] = microscopeSignal
                        }
                    }
                    else -> {
                        output.put(microscopeSignal)
                    }
                }
            }
            is RemoteMicroscopeStatus -> {}
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