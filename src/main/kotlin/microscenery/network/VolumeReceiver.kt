package microscenery.network

import graphics.scenery.utils.mapAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.lwjgl.system.MemoryUtil
import org.zeromq.ZContext
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

class VolumeReceiver(
    val volumeSize: Int,
    connections: Int = 30,
    basePort: Int = 4400,
    val reuseBuffers: Boolean = true,
    host: String = "localhost",
    zContext: ZContext
) {

    val buffers = graphics.scenery.utils.RingBuffer<ByteBuffer>(
        if (reuseBuffers) 2 else 0,
        default = { MemoryUtil.memAlloc(volumeSize) })

    val receivers = (basePort until basePort + connections).map {
        ChunkZMQReceiver(it, host, zContext)
    }.toList()

    fun getVolume(timeout: Long = 2000): ByteBuffer? {
        val slices = receivers.mapAsync {
            withContext(Dispatchers.IO) {
                it.outputQueue.poll(timeout, TimeUnit.MILLISECONDS)
            }
        }.toList()

        if (slices.all { it == null }) return null

        val buf = if (reuseBuffers) buffers.get() else MemoryUtil.memAlloc(volumeSize)
        buf.clear()

        slices.forEach { slice ->
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
            it.running = false
        }
    }

}