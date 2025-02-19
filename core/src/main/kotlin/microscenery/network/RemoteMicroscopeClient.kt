package microscenery.network

import fromScenery.lazyLogger
import me.jancasus.microscenery.network.v3.RemoteMicroscopeSignal
import microscenery.MicroscenerySettings
import microscenery.hardware.MicroscopeHardwareAgent
import microscenery.signals.*
import microscenery.signals.RemoteMicroscopeSignal.Companion.toPoko
import org.joml.Vector3f
import org.zeromq.ZContext
import java.util.concurrent.Future

/**
 * Is a virtual [MicroscopeHardware] to send commands to a remote microscope over network.
 * Translates [MicroscopeControlSignal]s to [BaseClientSignal]s to be received by [RemoteMicroscopeServer].
 *
 * Fetches slice data via [BiggishDataClient].
 */
class RemoteMicroscopeClient(
    basePort: Int = MicroscenerySettings.get("Network.basePort", 4000),
    host: String = MicroscenerySettings.get("Network.host", "localhost"),
    val zContext: ZContext,
    val nonMicroscopeMode: Boolean = false
) : MicroscopeHardwareAgent() {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private val controlConnection = ControlSignalsClient(zContext, basePort, host, listOf())
    private val sliceRequester = SliceRequester(controlConnection, listOf(this::processServerSignal))

    init {
        //startAgent() we dont need the agent, we just like to use the other stuff [MicroscopeHardwareAgent] brings
    }

    override fun snapSlice() {
        sendBaseWrappedSignal(MicroscopeControlSignal.SnapImage)
    }

    override fun moveStage(target: Vector3f) {
        sendBaseWrappedSignal(MicroscopeControlSignal.MoveStage(target))
    }

    override fun onLoop() {
        throw IllegalStateException("this agent does not need to be started")
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
        when (val s = unwrapToRemoteMicroscopeSignal(signal)) {
            is RemoteMicroscopeStatus -> {}
            is ActualMicroscopeSignal -> {
                when (val microscopeSignal = s.signal) {
                    is HardwareDimensions -> {
                        hardwareDimensions = microscopeSignal
                    }

                    is MicroscopeStatus -> {
                        if (microscopeSignal.state == ServerState.SHUTTING_DOWN) {
                            controlConnection.close()
                            sliceRequester.close()
                            this.close()
                        }
                        status = microscopeSignal
                    }

                    is MicroscopeStack ->{
                        if (nonMicroscopeMode && status.state == ServerState.STARTUP){
                            hardwareDimensions = HardwareDimensions(
                                Vector3f(-100f), Vector3f(100f),microscopeSignal.stack.imageMeta)
                            status = status.copy(state = ServerState.MANUAL)
                        }
                        output.put(microscopeSignal)
                    }

                    else -> {
                        if (microscopeSignal is MicroscopeSlice)
                            logger.info("Got slice ${microscopeSignal.slice.stackIdAndSliceIndex?.second} of stackIndex ${microscopeSignal.slice.stackIdAndSliceIndex?.first}")
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