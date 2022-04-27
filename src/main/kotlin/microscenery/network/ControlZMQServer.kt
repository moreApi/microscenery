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
 * Server shuts down when a signal with shutdown status has been send.
 */
class ControlZMQServer(val port: Int, val zContext: ZContext) {
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private val server = NetworkThread(port, this)
    val kryo = freeze()
    val thread = ZThread.fork(zContext, server)

    private val signalsOut = ArrayBlockingQueue<ServerSignal>(100)
    private val signalsIn = event<ClientSignal>()

    private val clients = mutableSetOf<String>()

    /**
     * Don't add too elaborate listeners. They get executed by the network thread.
     */
    fun addListener(listener: (ClientSignal) -> Unit) {
        synchronized(signalsIn){
            signalsIn += listener
        }
    }

    fun sendSignal(signal: ServerSignal){
        signalsOut.add(signal)
    }

    internal class NetworkThread(val port: Int, val parent: ControlZMQServer) : ZThread.IAttachedRunnable {

        override fun run(args: Array<Any>, ctx: ZContext, pipe: ZMQ.Socket) {
            val socket: ZMQ.Socket = ctx.createSocket(SocketType.ROUTER)
            socket.bind("tcp://*:$port")
            parent.logger.info("${ControlZMQServer::class.simpleName} bound to tcp://*:$port")

            var running = true

            while (!Thread.currentThread().isInterrupted && running) {

                // process incoming messages first.
                // First frame in each message is the sender identity
                var identity = socket.recvStr(ZMQ.DONTWAIT)
                while (identity != null){
                    parent.clients += identity
                    val payload = socket.recv()
                    val bin = ByteArrayInputStream(payload)
                    val input = Input(bin)
                    val event = parent.kryo.readClassAndObject(input) as? ClientSignal
                        ?: throw IllegalStateException("Received unknown, not ClientSignal payload")

                    synchronized(parent.signalsIn) {
                        parent.signalsIn(event)
                    }

                    identity = socket.recvStr(ZMQ.DONTWAIT)
                }

                // process outgoing messages
                var outSignal = parent.signalsOut.poll()
                while (outSignal != null){
                    val bos = ByteArrayOutputStream()
                    val output = Output(bos)
                    parent.kryo.writeClassAndObject(output, outSignal)
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

                    if (outSignal is ServerSignal.Status && outSignal.status == ServerStatus.ShuttingDown) {
                        running = false
                    }else {
                        outSignal = parent.signalsOut.poll()
                    }
                }
                Thread.sleep(200)
            }
            socket.linger = 0
            socket.close()
        }
    }
}