package microscenery.network

import graphics.scenery.utils.LazyLogger
import me.jancasus.microscenery.network.v2.ReplyHeaderSliceChunk
import me.jancasus.microscenery.network.v2.RequestSliceChunk
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZFrame
import org.zeromq.ZMQ
import kotlin.concurrent.thread

/**
 * Dumb server that answers requests for parts of slices from the storage.
 */
class BiggishDataServer(val port: Int, private val storage: SliceStorage, val zContext: ZContext) {
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

    val thread: Thread = networkThread()

    var running = true

    private val payload = ByteArray(CHUNK_SIZE)
    lateinit var router: ZMQ.Socket

    fun close(): Thread {
        running = false
        return thread
    }

    // taken from https://zguide.zeromq.org/docs/chapter7/#Transferring-Files
    //  The rest of the code is exactly the same as in model 2, except
    //  that we set the HWM on the server's ROUTER socket to PIPELINE
    //  to act as a sanity check.
    //  The server thread waits for a chunk request from a client,
    //  reads that chunk and sends it back to the client:
    private fun networkThread() = thread {
        router = zContext.createSocket(SocketType.ROUTER)
        router.hwm = PIPELINE * 2
        router.bind("tcp://*:$port")
        router.receiveTimeOut = 500
        logger.info("${BiggishDataServer::class.simpleName} bound to tcp://*:$port")

        while (!Thread.currentThread().isInterrupted && running) {
            //  First frame in each message is the sender identity
            val identity = ZFrame.recvFrame(router) ?: continue

            //  Second frame is request
            val request = RequestSliceChunk.parseFrom(router.recv())
            val replyBuilder = ReplyHeaderSliceChunk.newBuilder()
            replyBuilder.sliceId = request.sliceId

            val data = storage.getSlice(request.sliceId)?.duplicate()
            if (data == null) {
                logger.warn("Slice ${request.sliceId} was requested but was not found!")
                // requested slice can't be found
                val msg = replyBuilder.setSliceAvailable(false).build()
                identity.sendAndDestroy(router, ZMQ.SNDMORE)
                router.sendMore(msg.toByteArray())
                continue
            }
            replyBuilder.sliceAvailable = true

            data.position(data.position() + request.offset.coerceAtMost(data.remaining()))
            val size = request.chunkSize.coerceAtMost(data.remaining()).coerceAtMost(CHUNK_SIZE)

            val chunk = if (size == CHUNK_SIZE) {
                data.get(payload, 0, size)
                ZFrame(payload)
            } else {
                val smallerPayload = ByteArray(size)
                data.get(smallerPayload, 0, size)
                ZFrame(smallerPayload)
            }

            replyBuilder.chunkSize = size
            replyBuilder.offset = request.offset

            //  Send resulting chunk to client
            identity.sendAndDestroy(router, ZMQ.SNDMORE)
            router.sendMore(replyBuilder.build().toByteArray())
            chunk.sendAndDestroy(router) // does not actively free the memory. Just removes the pointer so GC can do its thing.
        }
        router.linger = 0
        router.close()
    }
}
