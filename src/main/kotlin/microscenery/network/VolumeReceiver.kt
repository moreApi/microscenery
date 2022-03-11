package microscenery.network

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import kotlin.math.min

class VolumeReceiver(
    val volumeSize: Int, connections: Int = 30, basePort: Int = 4400, val reuseBuffers: Boolean = true
) {
    val chunkSize = volumeSize / connections

    val buffers = graphics.scenery.utils.RingBuffer<ByteBuffer>(
        if (reuseBuffers) 2 else 0,
        default = { MemoryUtil.memAlloc(volumeSize) })

    val receivers = (basePort until basePort + connections).map {
        val a = ChunkUDPReceiver(it, 5)
        a.startReceiving()
        a
    }.toList()

    fun getVolume(timeout: Long = 2000): ByteBuffer? {
        val slices = receivers.map { it.outputQueue.poll(timeout, TimeUnit.MILLISECONDS) }.toList()

        if (slices.all { it == null }) return null

        val buf = if (reuseBuffers) buffers.get() else MemoryUtil.memAlloc(volumeSize)
        var lostChunkCounter = 0
        slices.forEach { queue ->
            if (queue == null) {
                if (reuseBuffers) {
                    buf.position(buf.position() + chunkSize)
                } else {
                    for (i in 0 until min(chunkSize, buf.remaining())) {
                        buf.put(0)
                    }
                }
                lostChunkCounter++
                return@forEach
            }

            var lostFragmentCounter = 0
            var counter = 0
            var fragment: VolumeFragment? = queue.poll()
            while (fragment != null) {
                if (counter < fragment.id) {
                    lostFragmentCounter += fragment.id - counter
                    // skipping missing byte
                    if (reuseBuffers) {
                        buf.position(buf.position() + (fragment.id - counter) * FRAGMENT_PAYLOAD_SIZE)
                    } else {
                        for (i in 0 until (fragment.id - counter) * FRAGMENT_PAYLOAD_SIZE) {
                            buf.put(0)
                        }
                    }
                    counter = fragment.id
                }


                if (buf.remaining() < fragment.data.remaining()) fragment.data.limit(fragment.data.position() + buf.remaining())
                buf.put(fragment.data)

                counter++
                fragment = queue.poll()
            }
            println("lost $lostFragmentCounter fragments")
        }
        println("lost $lostChunkCounter chunks")
        while (buf.hasRemaining() && !reuseBuffers) {
            // this only happens if the last fragment of the last connection is missing
            buf.put(0)
        }
        buf.rewind()
        return buf

    }

    /**
     * Starts closing all connections and threads.
     *
     * @return Job that waits on all threads to finish
     */
    fun close(): Job {
        return receivers.map {
            it.close()
        }.toList().let {
                runBlocking {
                    return@runBlocking launch {
                        it.forEach { it?.join() }
                    }
                }
            }
    }
}