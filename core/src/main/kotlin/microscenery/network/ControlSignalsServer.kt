package microscenery.network

import fromScenery.lazyLogger
import kotlinx.event.event
import me.jancasus.microscenery.network.v2.ClientSignal
import me.jancasus.microscenery.network.v2.EnumServerState
import microscenery.Agent
import microscenery.MicroscenerySettings
import microscenery.signals.ClientSignal.Companion.toPoko
import microscenery.signals.RemoteMicroscopeSignal
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * A server to receive [ClientSignal]s and send [RemoteMicroscopeSignal]s from.
 *
 * Server shuts down when a signal with shutdown status has been send.
 */
class ControlSignalsServer(
    zContext: ZContext, val port: Int = MicroscenerySettings.get("Network.basePort"),
    listeners: List<(microscenery.signals.ClientSignal) -> Unit> = emptyList()
) : Agent() {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private val socket: ZMQ.Socket

    private val signalsOut = ArrayBlockingQueue<me.jancasus.microscenery.network.v2.RemoteMicroscopeSignal>(1000)
    private val signalsIn = event<ClientSignal>()

    private val clients = mutableSetOf<ByteArray>()

    @Suppress("unused")
    val connectedClients
        get() = clients.size

    init {
        listeners.forEach { addListener(it) }

        socket = zContext.createSocket(SocketType.ROUTER)
        socket.bind("tcp://*:${port}")
        logger.info("${ControlSignalsServer::class.simpleName} bound to tcp://*:${port}")

        startAgent()
    }

    /**
     * Don't add too elaborate listeners. They get executed by the network thread.
     */
    fun addListener(listener: (microscenery.signals.ClientSignal) -> Unit) {
        synchronized(signalsIn) {
            signalsIn += { listener(it.toPoko()) }
        }
    }

    fun sendSignal(signal: RemoteMicroscopeSignal): Boolean {
        if (!signalsOut.offer(signal.toProto(), 5000, TimeUnit.MILLISECONDS)) {
            logger.warn("Dropped ${signal::class.simpleName} package because of full queue.")
            return false
        }
        return true
    }

    fun sendInternalSignals(signals: List<microscenery.signals.ClientSignal>) {
        synchronized(signalsIn) {
            signals.forEach { signalsIn(it.toProto()) }
        }
    }

    override fun onLoop() {
        val identity = socket.recv(ZMQ.DONTWAIT)
        val outSignal = signalsOut.poll()

        // process incoming messages first.
        // First frame in each message is the sender identity
        if (identity != null) {
            clients += identity
            val event = ClientSignal.parseFrom(socket.recv())

            synchronized(signalsIn) {
                signalsIn(event)
            }
        }

        // process outgoing messages
        if (outSignal != null) {
            val payload = outSignal.toByteArray()

            // publish to all clients
            clients.forEach { ident ->
                val isQueued = socket.sendMore(ident) && socket.send(payload)
                if (!isQueued) logger.error("ZMQ dropped messages :(")
                Thread.sleep(1)
            }

            if (outSignal.hasMicroscopeSignal()
                && outSignal.microscopeSignal.hasStatus()
                && outSignal.microscopeSignal.status.state == EnumServerState.SERVER_STATE_SHUTTING_DOWN
            ) {
                this.close()
            }
        }

        if (identity == null && outSignal == null)
            Thread.sleep(200)
    }

    override fun onClose() {
        socket.linger = 0
        socket.close()
    }


}