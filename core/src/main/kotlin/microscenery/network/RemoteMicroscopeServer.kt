package microscenery.network

import fromScenery.lazyLogger
import microscenery.Agent
import microscenery.MicroscenerySettings
import microscenery.hardware.MicroscopeHardware
import microscenery.signals.*
import microscenery.signals.MicroscopeControlSignal.Companion.toPoko
import org.joml.Vector3f
import org.zeromq.ZContext
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
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
            is MicroscopeSlice -> {
                signal.slice.data?.let {
                    storage.addSlice(signal.slice.Id, signal.slice.data)
                    controlConnection.sendSignal(ActualMicroscopeSignal(MicroscopeSlice(signal.slice.copy(data = null))))
                }
            }

            else -> controlConnection.sendSignal(ActualMicroscopeSignal(signal))
        }
    }

    /**
     * Executed by the network thread of [ControlSignalsServer]
     */
    private fun processClientSignal(bcs: BaseClientSignal) {
        when (bcs) {
            BaseClientSignal.ClientSignOn -> {
                status = status.copy(connectedClients = status.connectedClients + 1)
                controlConnection.sendSignal(ActualMicroscopeSignal(microscope.hardwareDimensions()))
                controlConnection.sendSignal(ActualMicroscopeSignal(microscope.status()))
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
    }
}