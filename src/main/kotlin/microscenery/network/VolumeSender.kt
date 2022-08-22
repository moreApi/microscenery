package microscenery.network

import MicroscenerySettings
import org.zeromq.ZContext
import java.nio.ByteBuffer

class VolumeSender(
    zContext: ZContext,
    val connections: Int = MicroscenerySettings.get("Network.connections"),
    val basePort: Int = MicroscenerySettings.get("Network.basePort")
) {
    val senders = (basePort until basePort + connections).map {
        ChunkZMQSender(it, zContext)
    }.toList()

    fun usedPorts() = senders.map { it.port }

    fun sendVolume(buffer: ByteBuffer) {
        val chunkSize = buffer.remaining() / connections

        senders.forEachIndexed { i, sender ->
            if (i != connections)
                buffer.limit(buffer.position() + chunkSize)
            else
                buffer.limit(buffer.capacity())
//            println("Sending from ${buffer.position()} to ${buffer.limit()}")
            buffer.mark()
            sender.sendBuffer(buffer.duplicate())
            buffer.position(buffer.position() + chunkSize)
        }
    }

    /**
     * Starts closing all connections and threads.
     *
     * @return closing connection threads that can be joined on
     */
    fun close() = senders.map {
            it.close()
        }.toList()
}