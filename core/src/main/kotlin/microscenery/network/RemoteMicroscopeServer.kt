package microscenery.network

import fromScenery.lazyLogger
import microscenery.Agent
import microscenery.MicroscenerySettings
import microscenery.hardware.MicroscopeHardware
import microscenery.signals.*
import org.zeromq.ZContext
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

@Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")
class RemoteMicroscopeServer @JvmOverloads constructor(
    val microscope: MicroscopeHardware,
    private val zContext: ZContext,
    val storage: SliceStorage = SliceStorage(),
    val basePort: Int = MicroscenerySettings.get("Network.basePort",4000),
    val connections: Int = MicroscenerySettings.get("Network.connections", 1),
) : Agent(false) {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private val controlConnection = ControlSignalsServer(zContext, basePort, listOf(this::processClientSignal))
    val dataSender = BiggishDataServer(basePort + 1, storage, zContext)

    var status: RemoteMicroscopeStatus by Delegates.observable(
        RemoteMicroscopeStatus(emptyList(), 0)
    ) { _, _, newStatus: RemoteMicroscopeStatus ->
        controlConnection.sendSignal(newStatus)
    }

    init {
        if (connections != 1) logger.warn("More than one data connection are currently not supported. Config asks for $connections")

        status = RemoteMicroscopeStatus(listOf(dataSender.port), 0)
        startAgent()
    }

    override fun onLoop() {
        val signal = microscope.output.poll(200, TimeUnit.MILLISECONDS) ?: return

        when (signal) {
            is Slice -> {
                signal.data?.let {
                    storage.addSlice(signal.Id, signal.data)
                    controlConnection.sendSignal(ActualMicroscopeSignal(signal.copy(data = null)))
                }
            }
            else -> controlConnection.sendSignal(ActualMicroscopeSignal(signal))
        }
    }

    /**
     * Executed by the network thread of [ControlSignalsServer]
     */
    private fun processClientSignal(it: ClientSignal) {
        when (it) {
            is ClientSignal.AcquireStack -> {
                microscope.acquireStack(it)
            }
            ClientSignal.ClientSignOn -> {
                status = status.copy(connectedClients = status.connectedClients + 1)
                controlConnection.sendSignal(ActualMicroscopeSignal(microscope.hardwareDimensions()))
                controlConnection.sendSignal(ActualMicroscopeSignal(microscope.status()))
            }
            ClientSignal.Live -> microscope.goLive()
            is ClientSignal.MoveStage -> microscope.stagePosition = it.target
            ClientSignal.Shutdown -> {
                logger.info("Shutting down server.")
                microscope.shutdown()
                close()
            }
            ClientSignal.SnapImage -> microscope.snapSlice()
            ClientSignal.Stop -> microscope.stop()
            is ClientSignal.AblationPoints -> microscope.ablatePoints(it)
            is ClientSignal.AblationShutter -> TODO()
            ClientSignal.StartAcquisition -> microscope.startAcquisition()
            is ClientSignal.DeviceSpecific -> microscope.deviceSpecificCommands(it.data)
        }
    }

    @Suppress("unused")
    fun stop() {
        logger.info("Got stop Command")
        controlConnection.sendInternalSignals(listOf(ClientSignal.Stop))
    }

    @Suppress("unused")
    fun shutdown() {
        logger.info("Got Stop Command")
        controlConnection.sendInternalSignals(listOf(ClientSignal.Shutdown))
    }

    /**
     * Access settings. Java compatability function
     */
    @Suppress("unused")
    fun getSettings() = MicroscenerySettings

    override fun onClose() {
        dataSender.close().join()
    }
}