package anon.network

import fromScenery.lazyLogger
import kotlinx.event.event
import anon.Agent
import anon.signals.BaseServerSignal
import anon.signals.Slice
import org.lwjgl.system.MemoryUtil
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Utility Agent that can be put between [ControlSignalsClient.addListener] and the actual consumer to fetch [Slice]s
 * via [BiggishDataClient].
 *
 * All [BaseServerSignal]s are piped though untouched except [Slice] signals. Those are cached and their data is requested.
 * Once the data is there they also get piped downstream together with their data.
 */
class SliceRequester(
    controlSignalsClient: ControlSignalsClient,
    listeners: List<(BaseServerSignal) -> Unit> = emptyList()
) : Agent() {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))


    private val signalsIn = event<BaseServerSignal>()

    private val dataConnection = BiggishDataClient(
        controlSignalsClient.zContext, controlSignalsClient.port + 1, controlSignalsClient.host
    )

    private val requestedSlices = ConcurrentHashMap<Int, Slice>()

    init {
        listeners.forEach { addListener(it) }
        controlSignalsClient.addListener(this::processServerSignal)
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

    override fun onLoop() {
        val sliceParts = dataConnection.outputQueue.poll(200, TimeUnit.MILLISECONDS) ?: return
        val meta = requestedSlices[sliceParts.id]

        if (meta == null) {
            logger.warn("Got data for slice ${sliceParts.id} but it was not requested.")
            return
        }
        if (sliceParts.size != meta.size) {
            logger.error("Size mismatch for slice ${sliceParts.id} ${sliceParts.size} vs ${meta.size}")
        }

        val buffer = MemoryUtil.memAlloc(sliceParts.size)
        sliceParts.chunks.forEach {
            buffer.put(it.value)
        }
        buffer.flip()
        synchronized(signalsIn) {
            signalsIn(meta.copy(data = buffer))
        }
    }

    /**
     * Executed by the network thread of [ControlSignalsClient]
     */
    private fun processServerSignal(signal: BaseServerSignal) {
        when (signal) {
            is Slice -> {
                if (dataConnection.requestSlice(signal.Id, signal.size)) {
                    // save signal for eventual data receiving
                    requestedSlices[signal.Id] = signal
                }
            }

            else -> {
                //we don't care, just hand it the next one in line
                synchronized(signalsIn) {
                    signalsIn(signal)
                }
            }
        }
    }

    override fun onClose() {
        dataConnection.close().join()
    }
}