package microscenery.network

import fromScenery.lazyLogger
import kotlinx.event.event
import me.jancasus.microscenery.network.v3.MicroscopeControlSignal
import microscenery.Agent
import microscenery.signals.*
import microscenery.signals.BaseServerSignal.Companion.toPoko
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * A Client to send control [MicroscopeControlSignal]s to [ControlSignalsServer] and receive [RemoteMicroscopeSignal]s.
 *
 * Client shuts down when a signal with shutdown status has been received.
 */
class ControlSignalsClient(
    zContext: ZContext,
    val port: Int,
    host: String,
    listeners: List<(BaseServerSignal) -> Unit> = emptyList()
) : Agent() {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private val socket: ZMQ.Socket

    private val signalsOut = ArrayBlockingQueue<org.withXR.network.v3.BaseClientSignal>(1000)
    private val signalsIn = event<BaseServerSignal>()

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

        signalsOut += BaseClientSignal.ClientSignOn.toProto()

        startAgent()
    }

    /**
     * Don't add too elaborate listeners. They get executed by the network thread.
     */
    fun addListener(listener: (BaseServerSignal) -> Unit) {
        synchronized(signalsIn) {
            signalsIn += { listener(it) }
        }
    }

    fun sendSignal(signal: BaseClientSignal): Boolean {
        if (!signalsOut.offer(signal.toProto(), 5000, TimeUnit.MILLISECONDS)) {
            logger.warn("Dropped ${signal::class.simpleName} package because of full queue.")
            return false
        }
        return true
    }

    override fun onLoop() {

        val payloadIn = socket.recv(ZMQ.DONTWAIT)
        val outSignal = signalsOut.poll()

        // process incoming messages first.
        // First frame in each message is the sender identity
        if (payloadIn != null) {
            val event = org.withXR.network.v3.BaseServerSignal.parseFrom(payloadIn)

            synchronized(signalsIn) {
                signalsIn(event.toPoko())
            }
        }

        // process outgoing messages
        if (outSignal != null) {
            if (!socket.send(outSignal.toByteArray())) {
                logger.error("ZMQ is busy and dropped a message")
            }
        }

        if (payloadIn == null && outSignal == null)
            Thread.sleep(200)
    }

    override fun onClose() {
        socket.linger = 0
        socket.close()

    }
}