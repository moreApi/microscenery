package microscenery.network

import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import freeze
import graphics.scenery.utils.LazyLogger
import kotlinx.event.event
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import org.zeromq.ZThread
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.ArrayBlockingQueue

/**
 *
 *
 * Shuts down when a signal with shutdown status has been received.
 */
class ControlZMQClient(
    val zContext: ZContext,
    val port: Int,
    val host: String,
    listeners: List<(ServerSignal) -> Unit> = emptyList()
) {
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

    val kryo = freeze()
    val thread: ZMQ.Socket

    private val signalsOut = ArrayBlockingQueue<ClientSignal>(100)
    private val signalsIn = event<ServerSignal>()

    init {
        listeners.forEach { signalsIn += it }
        thread = ZThread.fork(zContext, NetworkThread(this))
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

    private class NetworkThread(val parent: ControlZMQClient) : ZThread.IAttachedRunnable {

        override fun run(args: Array<Any>, ctx: ZContext, pipe: ZMQ.Socket) {
            val timeout = 200 //ms
            val socket: ZMQ.Socket = ctx.createSocket(SocketType.DEALER)
            socket.receiveTimeOut = timeout
            if (socket.connect("tcp://${parent.host}:${parent.port}")) {
                parent.logger.info("${ControlZMQClient::class.simpleName} connected to tcp://${parent.host}:${parent.port}")
            } else {
                throw IllegalStateException("Could not connect to ${ControlZMQClient::class.simpleName} connected to tcp://${parent.host}:${parent.port}")
            }

            var running = true
            parent.signalsOut += ClientSignal.ClientSignOn()

            while (!Thread.currentThread().isInterrupted && running) {

                // process incoming messages first.
                // First frame in each message is the sender identity
                var payloadIn = socket.recv(ZMQ.DONTWAIT)
                while (payloadIn != null){
                    val bin = ByteArrayInputStream(payloadIn)
                    val input = Input(bin)
                    val event = parent.kryo.readClassAndObject(input) as? ServerSignal
                        ?: throw IllegalStateException("Received unknown, not ClientSignal payload")

                    synchronized(parent.signalsIn) {
                        parent.signalsIn(event)
                    }

                    if (event is ServerSignal.Status && event.state == ServerState.ShuttingDown)
                        running = false

                    payloadIn = socket.recv(ZMQ.DONTWAIT)
                }

                // process outgoing messages
                var outSignal = parent.signalsOut.poll()
                while (outSignal != null && running){
                    val bos = ByteArrayOutputStream()
                    val output = Output(bos)
                    parent.kryo.writeClassAndObject(output, outSignal)
                    output.flush()

                    val payloadOut = bos.toByteArray()
                    socket.send(payloadOut)
                    Thread.sleep(1)

                    output.close()
                    bos.close()

                    outSignal = parent.signalsOut.poll()
                }
                Thread.sleep(200)
            }
            socket.linger = 0
            socket.close()
        }
    }
}