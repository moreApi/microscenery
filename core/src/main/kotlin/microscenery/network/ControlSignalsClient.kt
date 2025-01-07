package microscenery.network

import fromScenery.lazyLogger
import kotlinx.event.event
import me.jancasus.microscenery.network.v3.MicroscopeControlSignal
import microscenery.Agent
import microscenery.signals.*
import microscenery.signals.BaseServerSignal.Companion.toPoko
import microscenery.signals.RemoteMicroscopeSignal.Companion.toPoko
import org.withXR.network.v3.BaseServerSignal
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
    listeners: List<(RemoteMicroscopeSignal) -> Unit> = emptyList()
) : Agent() {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private val socket: ZMQ.Socket

    private val signalsOut = ArrayBlockingQueue<MicroscopeControlSignal>(1000)
    private val signalsIn = event<RemoteMicroscopeSignal>()

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

        signalsOut += MicroscopeControlSignal.newBuilder().run {
            clientSignOnBuilder.build()
            build()
        }

        startAgent()
    }

    /**
     * Don't add too elaborate listeners. They get executed by the network thread.
     */
    fun addListener(listener: (RemoteMicroscopeSignal) -> Unit) {
        synchronized(signalsIn) {
            signalsIn += { listener(it) }
        }
    }

    fun sendSignal(signal: microscenery.signals.MicroscopeControlSignal): Boolean {
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
            val event = unwrapBaseSignalToRemoteMicroscopeSignal(BaseServerSignal.parseFrom(payloadIn))

            synchronized(signalsIn) {
                signalsIn(event)
            }

            if (event is ActualMicroscopeSignal
                && event.signal is MicroscopeStatus
                && event.signal.state == ServerState.SHUTTING_DOWN
            ) {
                close()
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

    companion object {
        fun unwrapBaseSignalToRemoteMicroscopeSignal(signal: BaseServerSignal): RemoteMicroscopeSignal {
            val s = signal.toPoko()
            return when (s) {
                is microscenery.signals.BaseServerSignal.AppSpecific -> {
                    val data = signal.appSpecific.data
                    val rms = me.jancasus.microscenery.network.v3.RemoteMicroscopeSignal.parseFrom(data)
                    rms.toPoko()
                }

                is Slice -> ActualMicroscopeSignal(MicroscopeSlice(s))
                is Stack -> ActualMicroscopeSignal(MicroscopeStack(s))
            }
        }
    }
}