package microscenery.network

import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import freeze
import getPropertyInt
import graphics.scenery.utils.LazyLogger
import kotlinx.event.event
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread

/**
 * A server to receive [ClientSignal]s and send [ServerSignal]s from.
 *
 * Server shuts down when a signal with shutdown status has been send.
 */
class ControlZMQServer(
    val zContext: ZContext, val port: Int = getPropertyInt("Network.basePort"),
    listeners: List<(ClientSignal) -> Unit> = emptyList()
) {
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

    val thread: Thread

    private val signalsOut = ArrayBlockingQueue<ServerSignal>(100)
    private val signalsIn = event<ClientSignal>()

    private val clients = mutableSetOf<ByteArray>()

    init {
        listeners.forEach { signalsIn += it }
        thread = networkThread(this)
    }

    /**
     * Don't add too elaborate listeners. They get executed by the network thread.
     */
    fun addListener(listener: (ClientSignal) -> Unit) {
        synchronized(signalsIn) {
            signalsIn += listener
        }
    }

    fun sendSignal(signal: ServerSignal) {
        signalsOut.add(signal)
    }

    fun sendInternalSignals(signals: List<ClientSignal>) {
        synchronized(signalsIn) {
            signals.forEach { signalsIn(it) }
        }
    }

    private fun networkThread(parent: ControlZMQServer) = thread {
        val socket: ZMQ.Socket = zContext.createSocket(SocketType.ROUTER)
        socket.bind("tcp://*:${parent.port}")
        parent.logger.info("${ControlZMQServer::class.simpleName} bound to tcp://*:${parent.port}")

        var running = true
        val kryoIn = freeze()
        val kryoOut = freeze()

        while (!Thread.currentThread().isInterrupted && running) {
            Thread.sleep(200)

            // process incoming messages first.
            // First frame in each message is the sender identity
            var identity = socket.recv(ZMQ.DONTWAIT)
            while (identity != null) {
                parent.clients += identity
                val payload = socket.recv()
                val bin = ByteArrayInputStream(payload)
                val input = Input(bin)
                val event = kryoIn.readClassAndObject(input) as? ClientSignal
                    ?: throw IllegalStateException("Received unknown, not ClientSignal payload")

                synchronized(parent.signalsIn) {
                    parent.signalsIn(event)
                }

                identity = socket.recv(ZMQ.DONTWAIT)
            }

            // process outgoing messages
            var outSignal = parent.signalsOut.poll()
            while (outSignal != null) {
                val bos = ByteArrayOutputStream()
                val output = Output(bos)
                kryoOut.writeClassAndObject(output, outSignal)
                output.flush()
                val payload = bos.toByteArray()

                // publish to all clients
                parent.clients.forEach { ident ->
                    socket.sendMore(ident)
                    socket.send(payload)
                    Thread.sleep(1)
                }

                output.close()
                bos.close()

                if (outSignal is ServerSignal.Status && outSignal.state == ServerState.ShuttingDown) {
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