package microscenery.network

import fromScenery.lazyLogger
import microscenery.Agent
import microscenery.MicroscenerySettings
import microscenery.hardware.MicroscopeHardware
import microscenery.signals.*
import microscenery.signals.MicroscopeControlSignal.Companion.toPoko
import org.joml.Vector3f
import org.zeromq.ZContext
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
 * Wraps an [MicroscopeHardware] and sends its output as [MicoscopeSignal]s wrapped in [BaseClientSignal].
 * Also puts captured slice data into [SliceStorage] to be requested by [BiggishDataClient].
 * 
 * @param acquireOnConnect send an empty acquire stack signal to microscope on client connect
 */
@Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")
class RemoteMicroscopeServer @JvmOverloads constructor(
    val microscope: MicroscopeHardware,
    private val zContext: ZContext,
    val storage: SliceStorage = SliceStorage(),
    val basePort: Int = MicroscenerySettings.get("Network.basePort", 4000),
    val connections: Int = MicroscenerySettings.get("Network.connections", 1),
    val acquireOnConnect: Boolean = false
) : Agent(false) {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private val controlConnection = ControlSignalsServer(zContext, basePort, listOf(this::processClientSignal))
    val dataSender = BiggishDataServer(basePort + 1, storage, zContext)
    val bonjourService = BonjourService()

    var status: RemoteMicroscopeStatus by Delegates.observable(
        RemoteMicroscopeStatus(emptyList(), 0)
    ) { _, _, newStatus: RemoteMicroscopeStatus ->
        sendBaseWrappedSignal(newStatus)
        logger.info("Status: $newStatus")
    }

    init {
        if (connections != 1) logger.warn("More than one data connection are currently not supported. Config asks for $connections")

        status = RemoteMicroscopeStatus(listOf(dataSender.port), 0)
        startAgent()
    }

    override fun onStart() {
        bonjourService.register(InetAddress.getLocalHost().hostName, basePort, "RemoteMicroscope")
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
    
    private fun sendBaseWrappedSignal(signal: RemoteMicroscopeSignal){
        val wrapped = when (signal) {
            is RemoteMicroscopeStatus -> {
                BaseServerSignal.AppSpecific(signal.toProto().toByteString())
            }
            is ActualMicroscopeSignal -> when (signal.signal) {
                is MicroscopeStack -> {
                    signal.signal.stack
                }
                is MicroscopeSlice -> {
                    signal.signal.slice
                }
                else -> {
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
                status = status.copy(connectedClients = status.connectedClients + 1)
                sendBaseWrappedSignal(ActualMicroscopeSignal(microscope.hardwareDimensions()))
                sendBaseWrappedSignal(ActualMicroscopeSignal(microscope.status()))
                if (acquireOnConnect){
                    microscope.acquireStack(MicroscopeControlSignal.AcquireStack(Vector3f(),Vector3f(),1f))
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
        bonjourService.close()
    }
}