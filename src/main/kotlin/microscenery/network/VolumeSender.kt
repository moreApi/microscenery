package microscenery.network

import getPropertyInt
import org.zeromq.ZContext
import java.nio.ByteBuffer

class VolumeSender(
    zContext: ZContext,
    val connections: Int = getPropertyInt("Network.connections"),
    val basePort: Int = getPropertyInt("Network.basePort")
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

        senders.forEach {
            it.close()
        }
    }
}