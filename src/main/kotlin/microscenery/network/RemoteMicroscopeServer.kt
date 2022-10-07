package microscenery.network

import graphics.scenery.utils.LazyLogger
import kotlinx.event.event
import microscenery.Agent
import microscenery.MicroscenerySettings
import microscenery.hardware.MicroscopeHardware
import org.zeromq.ZContext
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

// TODO: Put hardware commands worker in own class
@Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")
class RemoteMicroscopeServer @JvmOverloads constructor(
    val microscope: MicroscopeHardware,
    val basePort: Int = MicroscenerySettings.get("Network.basePort"),
    val connections: Int = MicroscenerySettings.get("Network.connections", 1),
    private val zContext: ZContext,
    val storage: SliceStorage
): Agent() {
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private val controlConnection = ControlSignalsServer(zContext, basePort)
    val dataSender = BiggishDataServer(basePort + 1, storage, zContext)



    val statusChange = event<ServerSignal.ServerStatus>()
    var status by Delegates.observable(
        ServerSignal.ServerStatus(
            ServerState.STARTUP, listOf(dataSender.port), controlConnection.connectedClients, microscope.hardwareDimensions()
        )
    ) { _, _, newStatus: ServerSignal.ServerStatus ->
        statusChange(newStatus)
    }
        private set

    init {
        if (connections != 1) logger.warn("More than one data connection are currently not supported. Config asks for $connections")

        statusChange += {
            controlConnection.sendSignal(it)
        }

        controlConnection.addListener(this::processClientSignal)

        status = status.copy(
            state = ServerState.MANUAL,
            hwDimensions = microscope.hardwareDimensions()
        )

        startAgent()
    }

    /**
     * Executed by the network thread of [ControlSignalsServer]
     */
    private fun processClientSignal(it: ClientSignal) {
        when (it) {
            is ClientSignal.AcquireStack -> TODO()
            ClientSignal.ClientSignOn ->  controlConnection.sendSignal(status)
            ClientSignal.Live -> TODO()
            is ClientSignal.MoveStage -> {
                microscope.stagePosition = it.target
            }
            ClientSignal.Shutdown -> {
                microscope.shutdown()
                close()
            }
            ClientSignal.SnapImage -> microscope.snapSlice()
            ClientSignal.Stop -> TODO()
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



    override fun onLoop() {
        val signal = microscope.output.poll(200,TimeUnit.MILLISECONDS) ?: return

        //TODO send

    }

    override fun onClose() {
        dataSender.close().join()
    }
}