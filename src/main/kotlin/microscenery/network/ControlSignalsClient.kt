package microscenery.network

import graphics.scenery.utils.LazyLogger
import kotlinx.event.event
import me.jancasus.microscenery.network.v2.ClientSignal
import me.jancasus.microscenery.network.v2.EnumServerState
import me.jancasus.microscenery.network.v2.ServerSignal
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread

/**
 * A Client to send control [ClientSignal]s to [ControlSignalsServer] and receive [ServerSignal]s.
 *
 * Client shuts down when a signal with shutdown status has been received.
 */
class ControlSignalsClient(
    val zContext: ZContext, val port: Int, val host: String, listeners: List<(ServerSignal) -> Unit> = emptyList()
) {
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

    val thread: Thread

    private val signalsOut = ArrayBlockingQueue<ClientSignal>(100)
    private val signalsIn = event<ServerSignal>()

    init {
        listeners.forEach { signalsIn += it }
        thread = networkThread(this)
    }

    /**
     * Don't add too elaborate listeners. They get executed by the network thread.
     */
    fun addListener(listener: (ServerSignal) -> Unit) {
        synchronized(signalsIn) {
            signalsIn += listener
        }
    }

    fun sendSignal(signal: ClientSignal) {
        signalsOut.add(signal)
    }

    private fun networkThread(parent: ControlSignalsClient) = thread {
        val timeout = 200 //ms

        val socket: ZMQ.Socket = zContext.createSocket(SocketType.DEALER)
        socket.receiveTimeOut = timeout
        if (socket.connect("tcp://${parent.host}:${parent.port}")) {
            parent.logger.info("${ControlSignalsClient::class.simpleName} connected to tcp://${parent.host}:${parent.port}")
        } else {
            throw IllegalStateException("Could not connect to ${ControlSignalsClient::class.simpleName} connected to tcp://${parent.host}:${parent.port}")
        }

        var running = true

        parent.signalsOut += ClientSignal.newBuilder().run {
            clientSignOnBuilder.build()
            build()
        }

        while (!Thread.currentThread().isInterrupted && running) {

            // process incoming messages first.
            // First frame in each message is the sender identity
            var payloadIn = socket.recv(ZMQ.DONTWAIT)
            while (payloadIn != null) {
                val event = ServerSignal.parseFrom(payloadIn)

                synchronized(parent.signalsIn) {
                    parent.signalsIn(event)
                }

                if (event.hasServerStatus() && event.serverStatus.state == EnumServerState.SERVER_STATE_SHUTTING_DOWN) {
                    running = false
                    payloadIn = null
                } else {
                    payloadIn = socket.recv(ZMQ.DONTWAIT)
                }
            }

            // process outgoing messages
            var outSignal = parent.signalsOut.poll()
            while (outSignal != null && running) {
                socket.send(outSignal.toByteArray())
                outSignal = parent.signalsOut.poll()
            }
            Thread.sleep(200)
        }
        socket.linger = 0
        socket.close()

    }
}