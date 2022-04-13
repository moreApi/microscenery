package microscenery.network

import getPropertyInt
import getPropertyString
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
    val connections: Int = getPropertyInt("Network.connections"),
    val basePort: Int = getPropertyInt("Network.basePort"),
    val host: String = getPropertyString("Network.host")
) {
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

    val buffers = graphics.scenery.utils.RingBuffer<ByteBuffer>(
        if (reuseBuffers) 2 else 0,
        default = { MemoryUtil.memAlloc(volumeSize) })

    val receivers = (basePort until basePort + connections).map {
        ChunkZMQReceiver(it, host, zContext)
    }.toList()

    fun getVolume(timeout: Long = 2000, buffer: ByteBuffer? = null): ByteBuffer? {
        val slices = receivers.mapAsync {
            withContext(Dispatchers.IO) {
                it.outputQueue.poll(timeout, TimeUnit.MILLISECONDS)
            }
        }.toList()

        if (slices.all { it == null }) {
            logger.warn("All slices I got from host $host : $basePort + $connections connections where empty ")
            return null
        }

        val buf = buffer ?: if (reuseBuffers) buffers.get() else MemoryUtil.memAlloc(volumeSize)
        buf.clear()

        slices.forEach { slice ->
            if (slice == null) {
                logger.warn("A slice is null. This should not happen?")
                return@forEach
            }
            slice.forEach {
                buf.put(it)
            }
        }

        buf.rewind()
        return buf

    }

    /**
     * Starts closing all connections and threads.
     */
    fun close() {
        receivers.forEach {
            it.close()
        }
    }

}