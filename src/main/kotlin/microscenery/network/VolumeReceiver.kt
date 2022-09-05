package microscenery.network

import microscenery.MicroscenerySettings
import graphics.scenery.utils.LazyLogger
import graphics.scenery.utils.mapAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.lwjgl.system.MemoryUtil
import org.zeromq.ZContext
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

/**
 * @param volumeSize in bytes
 */
class VolumeReceiver(
    val reuseBuffers: Boolean = true,
    zContext: ZContext,
    val volumeSize: Int,
    connections: List<Pair<String,Int>>
) {
    constructor(
        reuseBuffers: Boolean = true, zContext: ZContext, volumeSize: Int,
        connections: Int = MicroscenerySettings.get("Network.connections"),
        basePort: Int = MicroscenerySettings.get("Network.basePort"),
        host: String = MicroscenerySettings.get("Network.host")
    ) : this(
        reuseBuffers, zContext, volumeSize,
        (0 until connections).map { host to basePort + it }
    )


    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

    val buffers = graphics.scenery.utils.RingBuffer<ByteBuffer>(
        if (reuseBuffers) 2 else 0,
        default = { MemoryUtil.memAlloc(volumeSize) })

    val receivers = connections.map {
        BiggishDataClient(zContext, it.second, it.first)
    }.toList()

    fun getVolume(timeout: Long = 2000, buffer: ByteBuffer? = null): ByteBuffer? {
        val slices = receivers.mapAsync {
            withContext(Dispatchers.IO) {
                it.outputQueue.poll(timeout, TimeUnit.MILLISECONDS)
            }
        }.toList()

        if (slices.all { it == null }) {
            logger.warn("All slices I got from host ${receivers.first().host}:${receivers.first().port} * ${receivers.size} connections was empty ")
            return null
        }

        val buf = buffer ?: if (reuseBuffers) buffers.get() else MemoryUtil.memAlloc(volumeSize)
        buf.clear()

        slices.forEach { slice ->
            if (slice == null) {
                logger.warn("A slice is null. This should not happen?")
                return@forEach
            }
            // TODO
            //slice.forEach {
            //    buf.put(it)
            //}
        }

        buf.rewind()
        return buf

    }

    /**
     * Starts closing all connections and threads.
     * @return closing connection threads that can be joined on
     */
    fun close() = receivers.map {
        it.close()
    }.toList()

}