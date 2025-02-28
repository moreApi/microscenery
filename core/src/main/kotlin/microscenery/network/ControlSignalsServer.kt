package microscenery.network

import fromScenery.lazyLogger
import kotlinx.event.event
import microscenery.Agent
import microscenery.MicroscenerySettings
import microscenery.signals.BaseClientSignal
import microscenery.signals.BaseClientSignal.Companion.toPoko
import microscenery.signals.BaseServerSignal
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * A server to receive [BaseClientSignal]s and send [BaseServerSignal]s from.
 *
 * Tries the send the rest of the queue once shutdown is = true.
 *
 * Receive [BaseClientSignal]s via subscribing with a listener by [addListener].
 * Don't add too elaborate listeners. They get executed by the network thread.
 *
 * Send via [sendSignal].
 */
class ControlSignalsServer(
    zContext: ZContext, val port: Int = MicroscenerySettings.get("Network.basePort", 4000),
    listeners: List<(BaseClientSignal) -> Unit> = emptyList()
) : Agent() {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private val socket: ZMQ.Socket

    private val signalsOut = ArrayBlockingQueue<org.withXR.network.v3.BaseServerSignal>(1000)
    private val signalsIn = event<BaseClientSignal>()

    private val clients = mutableSetOf<ByteArray>()

    internal var shutdown = false

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
    fun addListener(listener: (BaseClientSignal) -> Unit) {
        synchronized(signalsIn) {
            signalsIn += { listener(it) }
        }
    }

    /** Queues signal to be sent in main loop */
    fun sendSignal(signal: BaseServerSignal): Boolean {
        if (!signalsOut.offer(signal.toProto(), 5000, TimeUnit.MILLISECONDS)) {
            logger.warn("Dropped ${signal::class.simpleName} package because of full queue.")
            return false
        }
        return true
    }

    fun sendInternalSignals(signals: List<BaseClientSignal>) {
        synchronized(signalsIn) {
            signals.forEach { signalsIn(it) }
        }
    }

    override fun onLoop() {
        val identity = socket.recv(ZMQ.DONTWAIT)
        val outSignal = signalsOut.poll()

        // process incoming messages first.
        // First frame in each message is the sender identity
        if (identity != null) {
            clients += identity
            val event = org.withXR.network.v3.BaseClientSignal.parseFrom(socket.recv())

            synchronized(signalsIn) {
                signalsIn(event.toPoko())
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


        }

        if (identity == null && outSignal == null)
            Thread.sleep(200)
        if (shutdown) {
            this.close()
        }
    }

    override fun onClose() {
        socket.linger = 0
        socket.close()
    }


}