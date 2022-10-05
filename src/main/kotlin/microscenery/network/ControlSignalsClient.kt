package microscenery.network

import graphics.scenery.utils.LazyLogger
import kotlinx.event.event
import me.jancasus.microscenery.network.v2.ClientSignal
import me.jancasus.microscenery.network.v2.EnumServerState
import me.jancasus.microscenery.network.v2.ServerSignal
import microscenery.Agent
import microscenery.network.ServerSignal.Companion.toPoko
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.util.concurrent.ArrayBlockingQueue

/**
 * A Client to send control [ClientSignal]s to [ControlSignalsServer] and receive [ServerSignal]s.
 *
 * Client shuts down when a signal with shutdown status has been received.
 */
class ControlSignalsClient(
    zContext: ZContext, val port: Int, host: String, listeners: List<(microscenery.network.ServerSignal) -> Unit> = emptyList()
) : Agent() {
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private val socket: ZMQ.Socket

    private val signalsOut = ArrayBlockingQueue<ClientSignal>(100)
    private val signalsIn = event<ServerSignal>()

    init {
        listeners.forEach { addListener(it) }

        val timeout = 200 //ms

        socket = zContext.createSocket(SocketType.DEALER)
        socket.receiveTimeOut = timeout
        if (socket.connect("tcp://${host}:${port}")) {
            logger.info("${ControlSignalsClient::class.simpleName} connected to tcp://${host}:${port}")
        } else {
            throw IllegalStateException("Could not connect to ${ControlSignalsClient::class.simpleName} connected to tcp://${host}:${port}")
        }

        signalsOut += ClientSignal.newBuilder().run {
            clientSignOnBuilder.build()
            build()
        }

        startAgent()
    }

    /**
     * Don't add too elaborate listeners. They get executed by the network thread.
     */
    fun addListener(listener: (microscenery.network.ServerSignal) -> Unit) {
        synchronized(signalsIn) {
            signalsIn += { listener(it.toPoko()) }
        }
    }

    fun sendSignal(signal: microscenery.network.ClientSignal) {
        signalsOut.add(signal.toProto())
    }

    override fun onLoop() {

        // process incoming messages first.
        // First frame in each message is the sender identity
        var payloadIn = socket.recv(ZMQ.DONTWAIT)
        while (payloadIn != null) {
            val event = ServerSignal.parseFrom(payloadIn)

            synchronized(signalsIn) {
                signalsIn(event)
            }

            if (event.hasServerStatus() && event.serverStatus.state == EnumServerState.SERVER_STATE_SHUTTING_DOWN) {
                payloadIn = null
                close()
            } else {
                payloadIn = socket.recv(ZMQ.DONTWAIT)
            }
        }

        // process outgoing messages
        var outSignal = signalsOut.poll()
        while (outSignal != null && running) {
            socket.send(outSignal.toByteArray())
            outSignal = signalsOut.poll()
        }
        Thread.sleep(200)
    }

    override fun onClose() {
        socket.linger = 0
        socket.close()

    }
}