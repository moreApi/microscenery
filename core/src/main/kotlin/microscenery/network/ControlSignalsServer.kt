package microscenery.network

import fromScenery.lazyLogger
import kotlinx.event.event
import me.jancasus.microscenery.network.v3.MicroscopeControlSignal
import microscenery.Agent
import microscenery.MicroscenerySettings
import microscenery.signals.*
import microscenery.signals.BaseClientSignal.Companion.toPoko
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * A server to receive [MicroscopeControlSignal]s and send [RemoteMicroscopeSignal]s from.
 *
 * Server shuts down when a signal with shutdown status has been send.
 */
class ControlSignalsServer(
    zContext: ZContext, val port: Int = MicroscenerySettings.get("Network.basePort", 4000),
    listeners: List<(BaseClientSignal) -> Unit> = emptyList()
) : Agent() {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private val socket: ZMQ.Socket

    private val signalsOut = ArrayBlockingQueue<org.withXR.network.v3.BaseServerSignal>(1000)
    private val signalsIn = event<org.withXR.network.v3.BaseClientSignal>()

    private val clients = mutableSetOf<ByteArray>()

    private var shutdown = false

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
            signalsIn += { listener(it.toPoko()) }
        }
    }

    /** Queues signal to be sent in main loop */
    fun sendSignal(signal: RemoteMicroscopeSignal): Boolean {
        val wrapped = when (signal) {
            is RemoteMicroscopeStatus -> {
                BaseServerSignal.AppSpecific(signal.toProto().toByteString())
            }

            is ActualMicroscopeSignal -> when (signal.signal) {
                is MicroscopeStack -> {
                    signal.signal.stack
                }

                is MicroscopeSlice -> {
                    signal.signal.slice
                }
                else -> {
                    shutdown = signal.signal is MicroscopeStatus && signal.signal.state == ServerState.SHUTTING_DOWN
                    BaseServerSignal.AppSpecific(signal.toProto().toByteString())
                }
            }
        }

        if (!signalsOut.offer(wrapped.toProto(), 5000, TimeUnit.MILLISECONDS)) {
            logger.warn("Dropped ${signal::class.simpleName} package because of full queue.")
            return false
        }
        return true
    }

    fun sendInternalSignals(signals: List<BaseClientSignal>) {
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
            val event = org.withXR.network.v3.BaseClientSignal.parseFrom(socket.recv())

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