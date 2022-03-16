package microscenery.network

import org.zeromq.*
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue


internal const val PIPELINE = 10
internal const val CHUNK_SIZE = 250000

class ChunkZMQReceiver(port: Int, host: String = "localhost", zContext: ZContext) {

    val outputQueue = ArrayBlockingQueue<List<ByteBuffer>>(2)


    private val client = Client(outputQueue, port, host)
    val clientThread = ZThread.fork(zContext, client)
    var running
        get() = client.running
        set(value) {
            client.running = value
        }

    // taken from https://zguide.zeromq.org/docs/chapter7/#Transferring-Files
    internal class Client(
        val outputQueue: ArrayBlockingQueue<List<ByteBuffer>>, val port: Int, val host: String = "localhost"
    ) : ZThread.IAttachedRunnable {

        var running = true
        var collector = mutableListOf<ByteBuffer>()

        override fun run(args: Array<Any>, ctx: ZContext, pipe: ZMQ.Socket) {
            val dealer = ctx.createSocket(SocketType.DEALER)
            //            dealer.connect("tcp://127.0.0.1:6000")
            dealer.connect("tcp://$host:$port")


            var chunkId = 0.toByte()

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
                val chunk = ZFrame.recvFrame(dealer)
                credit++
                if (chunk.data[0] != chunkId) {
                    // this frame belongs to a finished chunk. Just reclaim the credit.
                    continue
                }
                val size = chunk.size() - 1 // subtract the id byte
                if (size != 0) {
                    chunks++
                    val buf = ByteBuffer.wrap(chunk.data)
                    buf.position(1)
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
                    chunkId = ((chunkId + 1) % 100).toByte() // just something that is not the old value
                } //  Last chunk received
            }
            pipe.send("OK")
        }
    }
}