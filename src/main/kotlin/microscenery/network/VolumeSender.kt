package microscenery.network

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

class VolumeSender(
    val connections: Int = 30, basePort: Int = 4400
) {
    val senders = (basePort until basePort + connections).map {
        val a = ChunkUDPSender(it)
        a.startSending()
        a
    }.toList()

    fun sendVolume(buffer: ByteBuffer) {
        val chunkSize = buffer.remaining() / connections

        senders.forEachIndexed { i, sender ->
            if (i != connections) buffer.limit(buffer.position() + chunkSize)
            else buffer.limit(buffer.capacity())
//            println("Sending from ${buffer.position()} to ${buffer.limit()}")
            sender.inputQueue.put(buffer.duplicate())
            buffer.position(buffer.position() + chunkSize)
        }
    }

    /**
     * Send a bit of data so the receivers think a new chunk starts and they finish the last chunk
     */
    fun dummyFinish() {
        val buf = MemoryUtil.memAlloc(10)
        println("Sending dummy finish")
        senders.forEach { it.inputQueue.put(buf.duplicate()) }
    }

//    suspend fun close() {
//        return senders.map {
//            it.close()
//        }.toList()
//            .let {
//                coroutineScope {
//                    it.forEach { it?.join() }
//                }
//            }
//    }

    /**
     * Starts closing all connections and threads.
     *
     * @return Job that waits on all threads to finish
     */
    fun close(): Job {
        return senders.map {
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