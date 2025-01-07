package microscenery.network

import fromScenery.lazyLogger
import me.jancasus.microscenery.network.v3.RemoteMicroscopeSignal
import microscenery.MicroscenerySettings
import microscenery.hardware.MicroscopeHardwareAgent
import microscenery.signals.*
import microscenery.signals.RemoteMicroscopeSignal.Companion.toPoko
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
        sendBaseWrappedSignal(MicroscopeControlSignal.SnapImage)
    }

    override fun moveStage(target: Vector3f) {
        sendBaseWrappedSignal(MicroscopeControlSignal.MoveStage(target))
    }

    override fun acquireStack(meta: MicroscopeControlSignal.AcquireStack) {
        sendBaseWrappedSignal(meta)
    }

    override fun ablatePoints(signal: MicroscopeControlSignal.AblationPoints) {
        sendBaseWrappedSignal(signal)
    }

    override fun startAcquisition() {
        sendBaseWrappedSignal(MicroscopeControlSignal.StartAcquisition)
    }

    override fun goLive() {
        sendBaseWrappedSignal(MicroscopeControlSignal.Live)
    }

    override fun sync(): Future<Boolean> {
        TODO("Not yet implemented")
    }

    override fun stop() {
        sendBaseWrappedSignal(MicroscopeControlSignal.Stop)
    }

    override fun deviceSpecificCommands(data: ByteArray) {
        sendBaseWrappedSignal(MicroscopeControlSignal.DeviceSpecific(data))
    }

    private fun sendBaseWrappedSignal(signal: MicroscopeControlSignal) =
        controlConnection.sendSignal(BaseClientSignal.AppSpecific(signal.toProto().toByteString()))

    /**
     * Executed by the network thread of [ControlSignalsClient]
     */
    private fun processServerSignal(signal: BaseServerSignal) {
        val s = unwrapToRemoteMicroscopeSignal(signal)

        when (s) {
            is RemoteMicroscopeStatus -> {}
            is ActualMicroscopeSignal -> {
                when (val microscopeSignal = s.signal) {
                    is HardwareDimensions -> {
                        hardwareDimensions = microscopeSignal
                    }

                    is MicroscopeStatus -> {
                        if (microscopeSignal.state == ServerState.SHUTTING_DOWN) {
                            controlConnection.close()
                            dataConnection.close()
                            this.close()
                        }
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
        }
    }

    private fun unwrapToRemoteMicroscopeSignal(signal: BaseServerSignal) = when (signal) {
        is BaseServerSignal.AppSpecific -> {
            val data = signal.data
            val rms = RemoteMicroscopeSignal.parseFrom(data)
            rms.toPoko()
        }

        is Slice -> ActualMicroscopeSignal(MicroscopeSlice(signal))
        is Stack -> ActualMicroscopeSignal(MicroscopeStack(signal))
    }

    @Suppress("unused")
    override fun shutdown() {
        logger.info("Got Stop Command")
        close()
    }

    override fun onClose() {
        sendBaseWrappedSignal(MicroscopeControlSignal.Shutdown)
    }
}