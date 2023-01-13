package microscenery.network

import graphics.scenery.utils.LazyLogger
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
    val basePort: Int = MicroscenerySettings.get("Network.basePort"),
    val connections: Int = MicroscenerySettings.get("Network.connections", 1),
) : Agent(false) {
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

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
            is HardwareDimensions -> controlConnection.sendSignal(ActualMicroscopeSignal(signal))
            is MicroscopeStatus -> controlConnection.sendSignal(ActualMicroscopeSignal(signal))
            is Slice -> {
                signal.data?.let {
                    storage.addSlice(signal.Id, signal.data)
                    controlConnection.sendSignal(ActualMicroscopeSignal(signal.copy(data = null)))
                }
            }
            is Stack -> TODO()
        }
    }

    /**
     * Executed by the network thread of [ControlSignalsServer]
     */
    private fun processClientSignal(it: ClientSignal) {
        when (it) {
            is ClientSignal.AcquireStack -> {
                logger.warn("Stacks are not implemented for remote at the moment.")
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