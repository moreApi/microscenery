package microscenery.network

import graphics.scenery.utils.LazyLogger
import kotlinx.event.event
import me.jancasus.microscenery.network.v2.ClientSignal
import me.jancasus.microscenery.network.v2.EnumServerState
import me.jancasus.microscenery.network.v2.ServerSignal
import microscenery.MicroscenerySettings
import microscenery.network.ClientSignal.Companion.toPoko
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread

/**
 * A server to receive [ClientSignal]s and send [ServerSignal]s from.
 *
 * Server shuts down when a signal with shutdown status has been send.
 */
class ControlSignalsServer(
    val zContext: ZContext, val port: Int = MicroscenerySettings.get("Network.basePort"),
    listeners: List<(ClientSignal) -> Unit> = emptyList()
) {
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

    val thread: Thread

    private val signalsOut = ArrayBlockingQueue<ServerSignal>(100)
    private val signalsIn = event<ClientSignal>()

    private val clients = mutableSetOf<ByteArray>()
    val connectedClients
        get() = clients.size

    init {
        listeners.forEach { signalsIn += it }
        thread = networkThread(this)
    }

    /**
     * Don't add too elaborate listeners. They get executed by the network thread.
     */
    fun addListener(listener: (microscenery.network.ClientSignal) -> Unit) {
        synchronized(signalsIn) {
            signalsIn += { listener(it.toPoko()) }
        }
    }

    fun sendSignal(signal: microscenery.network.ServerSignal) {
        signalsOut.add(signal.toProto())
    }

    fun sendInternalSignals(signals: List<microscenery.network.ClientSignal>) {
        synchronized(signalsIn) {
            signals.forEach { signalsIn(it.toProto()) }
        }
    }

    private fun networkThread(parent: ControlSignalsServer) = thread {
        val socket: ZMQ.Socket = zContext.createSocket(SocketType.ROUTER)
        socket.bind("tcp://*:${parent.port}")
        parent.logger.info("${ControlSignalsServer::class.simpleName} bound to tcp://*:${parent.port}")

        var running = true

        while (!Thread.currentThread().isInterrupted && running) {
            Thread.sleep(200)

            // process incoming messages first.
            // First frame in each message is the sender identity
            var identity = socket.recv(ZMQ.DONTWAIT)
            while (identity != null) {
                parent.clients += identity
                val event = ClientSignal.parseFrom(socket.recv())

                synchronized(parent.signalsIn) {
                    parent.signalsIn(event)
                }

                identity = socket.recv(ZMQ.DONTWAIT)
            }

            // process outgoing messages
            var outSignal = parent.signalsOut.poll()
            while (outSignal != null) {
                val payload = outSignal.toByteArray()

                // publish to all clients
                parent.clients.forEach { ident ->
                    socket.sendMore(ident)
                    socket.send(payload)
                    Thread.sleep(1)
                }

                if (outSignal.hasServerStatus()
                    && outSignal.serverStatus.state == EnumServerState.SERVER_STATE_SHUTTING_DOWN
                ) {
                    running = false
                    outSignal = null
                } else {
                    outSignal = parent.signalsOut.poll()
                }
            }
        }
        socket.linger = 0
        socket.close()

    }
}