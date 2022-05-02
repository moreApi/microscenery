package microscenery.network

import graphics.scenery.utils.LazyLogger
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZFrame
import org.zeromq.ZMQ
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread


internal const val PIPELINE = 10
internal const val CHUNK_SIZE = 250000

class ChunkZMQReceiver(val zContext: ZContext, val port: Int, val host: String = "localhost") {
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

    val thread: Thread = networkThread()

    val outputQueue = ArrayBlockingQueue<List<ByteBuffer>>(2)

    var running = true

    var collector = mutableListOf<ByteBuffer>()
    lateinit var dealer: ZMQ.Socket

    fun close(): Thread {
        running = false
        return thread
    }

    // taken from https://zguide.zeromq.org/docs/chapter7/#Transferring-Files
    private fun networkThread() = thread {
        dealer = zContext.createSocket(SocketType.DEALER)
        dealer.connect("tcp://$host:$port")
        dealer.receiveTimeOut = 500

        logger.info("${ChunkZMQReceiver::class.simpleName} connected to tcp://$host:$port")

        var chunkId = 0

        //  Up to this many chunks in transit
        var credit = PIPELINE
        var total = 0 //  Total bytes received
        var chunks = 0 //  Total chunks received
        var offset = 0 //  Offset of next chunk request
        while (running) {
            if (outputQueue.remainingCapacity() == 0) {
                Thread.sleep(200)
                continue
            }

            while (credit > 0) {
                //  Ask for next chunk
                dealer.sendMore("fetch")
                dealer.sendMore(offset.toString())
                dealer.sendMore(CHUNK_SIZE.toString())
                dealer.send(chunkId.toString())
                offset += CHUNK_SIZE
                credit--
            }
            credit++
            val recChunkId = dealer.recvStr()
            val chunk = ZFrame.recvFrame(dealer)
            if (recChunkId != chunkId.toString()) {
                // this frame belongs to a finished chunk. Just reclaim the credit.
                continue
            }

            val size = chunk.size()
            if (size != 0) {
                chunks++
                val buf = ByteBuffer.wrap(chunk.data)
                buf.position(0)
                buf.mark()
                collector.add(buf)
                chunk.destroy()
                total += size
            }
            if (size < CHUNK_SIZE) {
                //System.out.printf("%d chunks received, %d bytes\n", chunks, total)
                outputQueue.put(collector)
                collector = mutableListOf<ByteBuffer>()
                chunks = 0
                offset = 0
                total = 0 //  Total bytes received
                chunkId = ((chunkId + 1) % 100) // just something that is not the old value
            } //  Last chunk received
        }
        dealer.linger = 0
        dealer.close()
    }
}
