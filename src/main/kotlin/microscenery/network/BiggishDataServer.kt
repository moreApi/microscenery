package microscenery.network

import graphics.scenery.utils.LazyLogger
import me.jancasus.microscenery.network.v2.ReplyHeaderSliceChunk
import me.jancasus.microscenery.network.v2.RequestSliceChunk
import microscenery.Agent
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZFrame
import org.zeromq.ZMQ
import java.nio.ByteOrder

/**
 * Dumb server that answers requests for parts of slices from the storage.
 */
class BiggishDataServer(val port: Int, private val storage: SliceStorage, zContext: ZContext) : Agent() {
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private val router: ZMQ.Socket

    init {
        router = zContext.createSocket(SocketType.ROUTER)
        router.hwm = PIPELINE * 2
        router.bind("tcp://*:$port")
        router.receiveTimeOut = 500
        logger.info("${BiggishDataServer::class.simpleName} bound to tcp://*:$port")

        startAgent()
    }

    // taken from https://zguide.zeromq.org/docs/chapter7/#Transferring-Files
    //  The rest of the code is exactly the same as in model 2, except
    //  that we set the HWM on the server's ROUTER socket to PIPELINE
    //  to act as a sanity check.
    //  The server thread waits for a chunk request from a client,
    //  reads that chunk and sends it back to the client:
    override fun onLoop() {
        //  First frame in each message is the sender identity
        val identity = ZFrame.recvFrame(router) ?: return

        //  Second frame is request
        val request = RequestSliceChunk.parseFrom(router.recv())
        val replyBuilder = ReplyHeaderSliceChunk.newBuilder()
        replyBuilder.sliceId = request.sliceId

        val data = storage.getSlice(request.sliceId)?.duplicate()?.order(ByteOrder.LITTLE_ENDIAN)
        if (data == null) {
            logger.warn("Slice ${request.sliceId} was requested but was not found!")
            // requested slice can't be found
            val msg = replyBuilder.setSliceAvailable(false).build()
            identity.sendAndDestroy(router, ZMQ.SNDMORE)
            router.send(msg.toByteArray())
            return
        }
        replyBuilder.sliceAvailable = true

        data.position(data.position() + request.offset.coerceAtMost(data.remaining()))
        val size = request.chunkSize.coerceAtMost(data.remaining()).coerceAtMost(CHUNK_SIZE)

        //OPTIMIZATION POTENTIAL: introduce ring buffer or something to avoid creating new buffers constantly. Check out what ZFrame.destroy does.
        val payload = ByteArray(size) //can't easily reuse memory since zmq needs to handle it
        data.get(payload, 0, size)
        val chunk = ZFrame(payload)

        replyBuilder.chunkSize = size
        replyBuilder.offset = request.offset

        //  Send resulting chunk to client
        identity.sendAndDestroy(router, ZMQ.SNDMORE)
        router.sendMore(replyBuilder.build().toByteArray())
        chunk.sendAndDestroy(router) // does not actively free the memory. Just removes the pointer so GC can do its thing.
    }

    override fun onClose() {
        router.linger = 0
        router.close()
    }
}
