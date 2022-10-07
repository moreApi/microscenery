package microscenery.network

import graphics.scenery.utils.LazyLogger
import microscenery.MicroscenerySettings
import microscenery.hardware.MicroscopeHardware
import microscenery.hardware.MicroscopeHardwareAgent
import microscenery.signals.ClientSignal
import microscenery.signals.ServerState
import org.joml.Vector3f
import org.zeromq.ZContext
import java.util.concurrent.TimeUnit

// TODO: Put hardware commands worker in own class
@Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")
class RemoteMicroscopeServer @JvmOverloads constructor(
    val microscope: MicroscopeHardware,
    val basePort: Int = MicroscenerySettings.get("Network.basePort"),
    val connections: Int = MicroscenerySettings.get("Network.connections", 1),
    private val zContext: ZContext,
    val storage: SliceStorage
): MicroscopeHardwareAgent() {
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private val controlConnection = ControlSignalsServer(zContext, basePort)
    val dataSender = BiggishDataServer(basePort + 1, storage, zContext)




    init {
        if (connections != 1) logger.warn("More than one data connection are currently not supported. Config asks for $connections")

        //statusChange += {
        //    controlConnection.sendSignal(it)
        //}

        controlConnection.addListener(this::processClientSignal)

        status = status.copy(
            state = ServerState.MANUAL,
            //hwDimensions = microscope.hardwareDimensions()
        )

        startAgent()
    }

    /**
     * Executed by the network thread of [ControlSignalsServer]
     */
    private fun processClientSignal(it: ClientSignal) {
        when (it) {
            is ClientSignal.AcquireStack -> TODO()
            ClientSignal.ClientSignOn -> TODO()// controlConnection.sendSignal(status)
            ClientSignal.Live -> TODO()
            is ClientSignal.MoveStage -> {
                //microscope.stagePosition = it.target
            }
            ClientSignal.Shutdown -> {
                microscope.shutdown()
                close()
            }
            ClientSignal.SnapImage -> microscope.snapSlice(Vector3f())
            ClientSignal.Stop -> TODO()
        }
    }


    @Suppress("unused")
    fun stop() {
        logger.info("Got stop Command")
        controlConnection.sendInternalSignals(listOf(ClientSignal.Stop))
    }

    @Suppress("unused")
    override fun shutdown() {
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

    override fun snapSlice(target: Vector3f) {
        TODO("Not yet implemented")
    }
}