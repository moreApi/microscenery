package microscenery.network

import org.zeromq.ZContext
import java.nio.ByteBuffer

class VolumeSender(
    val connections: Int = 30, basePort: Int = 4400, zContext: ZContext
) {
    val senders = (basePort until basePort + connections).map {
        ChunkZMQSender(it, zContext)
    }.toList()

    fun sendVolume(buffer: ByteBuffer) {
        val chunkSize = buffer.remaining() / connections

        senders.forEachIndexed { i, sender ->
            if (i != connections) buffer.limit(buffer.position() + chunkSize)
            else buffer.limit(buffer.capacity())
//            println("Sending from ${buffer.position()} to ${buffer.limit()}")
            buffer.mark()
            sender.sendBuffer(buffer.duplicate())
            buffer.position(buffer.position() + chunkSize)
        }
    }

    /**
     * Starts closing all connections and threads.
     *
     * @return Job that waits on all threads to finish
     */
    fun close() {

        senders.forEach { it.running = false }
    }
}