package microscenery.network

import fromScenery.lazyLogger
import microscenery.Agent
import microscenery.MicroscenerySettings
import microscenery.Settings
import microscenery.hardware.MicroscopeHardware
import microscenery.signals.*
import microscenery.signals.MicroscopeControlSignal.Companion.toPoko
import org.joml.Vector3f
import org.zeromq.ZContext
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
 * Wraps an [MicroscopeHardware] and sends its output as [MicoscopeSignal]s wrapped in [BaseClientSignal].
 * Also puts captured slice data into [SliceStorage] to be requested by [BiggishDataClient].
 * @param acquireOnConnect tries to resend the last stack or trigger a new capture on client connect
 */
@Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")
class RemoteMicroscopeServer @JvmOverloads constructor(
    val microscope: MicroscopeHardware,
    private val zContext: ZContext,
    val storage: SliceStorage = SliceStorage(),
    val basePort: Int = MicroscenerySettings.get(Settings.Network.BasePort, 4000),
    val host: String = MicroscenerySettings.get(Settings.Network.Host,"*").trim(),
    val acquireOnConnect: Boolean = false,
    val announceWithBonjour: Boolean = MicroscenerySettings.get(Settings.Network.AnnounceBonjour,true),
    var serverHello: BaseServerSignal.ServerHello =
        BaseServerSignal.ServerHello("a microscope", ServerType.MICROSCOPE, "")
) : Agent(false) {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private val controlConnection = ControlSignalsServer(zContext, basePort, host = host, listOf(this::processClientSignal))
    val dataSender = BiggishDataServer(basePort + 1, host = host, storage, zContext)
    val bonjourService = if (announceWithBonjour) BonjourService() else null

    private var lastStack: Stack? = null
        set(value) {
            lastStackSlices = emptyList()
            field = value
        }
    private var lastStackSlices = emptyList<Slice>()

    var status: RemoteMicroscopeStatus by Delegates.observable(
        RemoteMicroscopeStatus(emptyList(), 0)
    ) { _, _, newStatus: RemoteMicroscopeStatus ->
        sendBaseWrappedSignal(newStatus)
        logger.info("Status: $newStatus")
    }

    init {
        status = RemoteMicroscopeStatus(listOf(dataSender.port), 0)
        startAgent()
    }

    override fun onStart() {
        bonjourService?.register(serverHello.serverName, basePort, "RemoteMicroscope")
    }

    override fun onLoop() {
        val signal = microscope.output.poll(200, TimeUnit.MILLISECONDS) ?: return

        when (signal) {
            is MicroscopeSlice -> {
                signal.slice.data?.let {
                    storage.addSlice(signal.slice.Id, signal.slice.data)
                    sendBaseWrappedSignal(ActualMicroscopeSignal(MicroscopeSlice(signal.slice.copy(data = null))))
                }
            }

            else -> sendBaseWrappedSignal(ActualMicroscopeSignal(signal))
        }
    }
    
    private fun sendBaseWrappedSignal(signal: RemoteMicroscopeSignal, isResend: Boolean = false){
        val wrapped = when (signal) {
            is RemoteMicroscopeStatus -> {
                BaseServerSignal.AppSpecific(signal.toProto().toByteString())
            }
            is ActualMicroscopeSignal -> when (signal.signal) {
                is MicroscopeStack -> {
                    lastStack = signal.signal.stack
                    signal.signal.stack
                }
                is MicroscopeSlice -> {
                    val slice = signal.signal.slice
                    if (slice.stackIdAndSliceIndex?.first == lastStack?.Id){
                        lastStackSlices += slice
                    }
                    slice

                }
                else -> {
                    if (signal.signal is HardwareDimensions && !isResend){
                        lastStack = null
                        lastStackSlices = emptyList()
                    }
                    BaseServerSignal.AppSpecific(signal.toProto().toByteString())
                }
            }
        }
        controlConnection.sendSignal(wrapped)
    }

    /**
     * Executed by the network thread of [ControlSignalsServer]
     */
    private fun processClientSignal(bcs: BaseClientSignal) {
        when (bcs) {
            BaseClientSignal.ClientSignOn -> {
                controlConnection.sendSignal(serverHello)
                status = status.copy(connectedClients = status.connectedClients + 1)
                sendBaseWrappedSignal(ActualMicroscopeSignal(microscope.hardwareDimensions()), isResend = true)
                sendBaseWrappedSignal(ActualMicroscopeSignal(microscope.status()), isResend = true)
                if (acquireOnConnect){
                    lastStack?.let { stack ->
                        logger.info("Resending last stack for new client")
                        controlConnection.sendSignal(stack)
                        lastStackSlices.forEach(controlConnection::sendSignal)
                    } ?: run {
                        logger.info("No previous stack found, acquiring new stack for new client.")
                        microscope.acquireStack(MicroscopeControlSignal.AcquireStack(Vector3f(), Vector3f(), 1f))
                    }
                }
            }

            is BaseClientSignal.AppSpecific -> {
                val it = me.jancasus.microscenery.network.v3.MicroscopeControlSignal.parseFrom(bcs.data).toPoko()
                when (it) {
                    is MicroscopeControlSignal.AcquireStack -> {
                        microscope.acquireStack(it)
                    }

                    MicroscopeControlSignal.Live -> microscope.goLive()
                    is MicroscopeControlSignal.MoveStage -> microscope.stagePosition = it.target
                    MicroscopeControlSignal.Shutdown -> {
                        logger.info("Shutting down server.")
                        microscope.shutdown()
                        controlConnection.shutdown = true
                        dataSender.close()
                        close()
                    }

                    MicroscopeControlSignal.SnapImage -> microscope.snapSlice()
                    MicroscopeControlSignal.Stop -> microscope.stop()
                    is MicroscopeControlSignal.AblationPoints -> microscope.ablatePoints(it)
                    is MicroscopeControlSignal.AblationShutter -> TODO()
                    MicroscopeControlSignal.StartAcquisition -> microscope.startAcquisition()
                    is MicroscopeControlSignal.DeviceSpecific -> microscope.deviceSpecificCommands(it.data)
                }
            }
        }
    }

    @Suppress("unused")
    fun stop() {
        logger.info("Got stop Command")
        controlConnection.sendInternalSignals(listOf(MicroscopeControlSignal.Stop.toBaseSignal()))
    }

    @Suppress("unused")
    fun shutdown() {
        logger.info("Got Stop Command")
        controlConnection.sendInternalSignals(listOf(MicroscopeControlSignal.Shutdown.toBaseSignal()))
    }

    /**
     * Access settings. Java compatability function
     */
    @Suppress("unused")
    fun getSettings() = MicroscenerySettings

    override fun onClose() {
        dataSender.close().join()
        bonjourService?.close()
    }
}