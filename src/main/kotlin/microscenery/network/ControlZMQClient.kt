package microscenery.network

import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import freeze
import graphics.scenery.utils.LazyLogger
import kotlinx.event.event
import org.zeromq.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.ArrayBlockingQueue

/**
 *
 *
 * Shuts down when a signal with shutdown status has been received.
 */
class ControlZMQClient(val port: Int, val zContext: ZContext) {
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private val server = NetworkThread(port, this)
    val kryo = freeze()
    val thread = ZThread.fork(zContext, server)

    private val signalsOut = ArrayBlockingQueue<ClientSignal>(100)
    private val signalsIn = event<ServerSignal>()

    /**
     * Don't add too elaborate listeners. They get executed by the network thread.
     */
    fun addListener(listener: (ServerSignal) -> Unit) {
        synchronized(signalsIn){
            signalsIn += listener
        }
    }

    fun sendSignal(signal: ClientSignal){
        signalsOut.add(signal)
    }

    internal class NetworkThread(val port: Int, val parent: ControlZMQClient) : ZThread.IAttachedRunnable {

        override fun run(args: Array<Any>, ctx: ZContext, pipe: ZMQ.Socket) {
            val socket: ZMQ.Socket = ctx.createSocket(SocketType.DEALER)
            socket.bind("tcp://*:$port")
            parent.logger.info("${ControlZMQClient::class.simpleName} bound to tcp://*:$port")

            var running = true

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

                    if (event is ServerSignal.Status && event.status == ServerStatus.ShuttingDown)
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