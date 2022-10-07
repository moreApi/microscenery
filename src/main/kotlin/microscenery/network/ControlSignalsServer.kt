package microscenery.network

import graphics.scenery.utils.LazyLogger
import kotlinx.event.event
import me.jancasus.microscenery.network.v2.ClientSignal
import me.jancasus.microscenery.network.v2.EnumServerState
import microscenery.Agent
import microscenery.MicroscenerySettings
import microscenery.network.ClientSignal.Companion.toPoko
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.util.concurrent.ArrayBlockingQueue

/**
 * A server to receive [ClientSignal]s and send [MicroscopeSignal]s from.
 *
 * Server shuts down when a signal with shutdown status has been send.
 */
class ControlSignalsServer(
    zContext: ZContext, val port: Int = MicroscenerySettings.get("Network.basePort"),
    listeners: List<(microscenery.network.ClientSignal) -> Unit> = emptyList()
) : Agent() {
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private val socket: ZMQ.Socket

    private val signalsOut = ArrayBlockingQueue<me.jancasus.microscenery.network.v2.RemoteMicroscopeSignal>(100)
    private val signalsIn = event<ClientSignal>()

    private val clients = mutableSetOf<ByteArray>()
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
    fun addListener(listener: (microscenery.network.ClientSignal) -> Unit) {
        synchronized(signalsIn) {
            signalsIn += { listener(it.toPoko()) }
        }
    }

    fun sendSignal(signal: RemoteMicroscopeSignal) {
        signalsOut.add(signal.toProto())
    }

    fun sendInternalSignals(signals: List<microscenery.network.ClientSignal>) {
        synchronized(signalsIn) {
            signals.forEach { signalsIn(it.toProto()) }
        }
    }

    override fun onLoop() {
        // process incoming messages first.
        // First frame in each message is the sender identity
        var identity = socket.recv(ZMQ.DONTWAIT)
        while (identity != null) {
            clients += identity
            val event = ClientSignal.parseFrom(socket.recv())

            synchronized(signalsIn) {
                signalsIn(event)
            }

            identity = socket.recv(ZMQ.DONTWAIT)
        }

        // process outgoing messages
        var outSignal = signalsOut.poll()
        while (outSignal != null) {
            val payload = outSignal.toByteArray()

            // publish to all clients
            clients.forEach { ident ->
                socket.sendMore(ident)
                socket.send(payload)
                Thread.sleep(1)
            }

            if (outSignal.hasMicroscopeSignal()
                && outSignal.microscopeSignal.hasStatus()
                && outSignal.microscopeSignal.status.state == EnumServerState.SERVER_STATE_SHUTTING_DOWN
            ) {
                outSignal = null
                this.close()
            } else {
                outSignal = signalsOut.poll()
            }
        }
        Thread.sleep(200)
    }

    override fun onClose() {
        socket.linger = 0
        socket.close()
    }


}