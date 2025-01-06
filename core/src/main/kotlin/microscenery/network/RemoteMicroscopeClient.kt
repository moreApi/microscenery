package microscenery.network

import fromScenery.lazyLogger
import microscenery.MicroscenerySettings
import microscenery.hardware.MicroscopeHardwareAgent
import microscenery.signals.*
import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil
import org.zeromq.ZContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 *
 */
class RemoteMicroscopeClient(
    basePort: Int = MicroscenerySettings.get("Network.basePort", 4000),
    host: String = MicroscenerySettings.get("Network.host", "localhost"),
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
        output.put(MicroscopeSlice(meta.copy(data = buffer)))
    }

    override fun snapSlice() {
        controlConnection.sendSignal(MicroscopeControlSignal.SnapImage)
    }

    override fun moveStage(target: Vector3f) {
        controlConnection.sendSignal(MicroscopeControlSignal.MoveStage(target))
    }

    override fun acquireStack(meta: MicroscopeControlSignal.AcquireStack) {
        controlConnection.sendSignal(meta)
    }

    override fun ablatePoints(signal: MicroscopeControlSignal.AblationPoints) {
        controlConnection.sendSignal(signal)
    }

    override fun startAcquisition() {
        controlConnection.sendSignal(MicroscopeControlSignal.StartAcquisition)
    }

    override fun goLive() {
        controlConnection.sendSignal(MicroscopeControlSignal.Live)
    }

    override fun sync(): Future<Boolean> {
        TODO("Not yet implemented")
    }

    override fun stop() {
        controlConnection.sendSignal(MicroscopeControlSignal.Stop)
    }

    override fun deviceSpecificCommands(data: ByteArray) {
        controlConnection.sendSignal(MicroscopeControlSignal.DeviceSpecific(data))
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

                    is MicroscopeSlice -> {
                        val slice = microscopeSignal.slice
                        if (dataConnection.requestSlice(slice.Id, slice.size)) {
                            // save signal for eventual data receiving
                            requestedSlices[slice.Id] = slice
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
        controlConnection.sendSignal(MicroscopeControlSignal.Shutdown)
    }
}