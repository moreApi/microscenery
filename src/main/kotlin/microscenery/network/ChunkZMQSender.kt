package microscenery.network

import graphics.scenery.utils.LazyLogger
import org.lwjgl.system.MemoryUtil
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZFrame
import org.zeromq.ZMQ
import java.nio.ByteBuffer
import kotlin.concurrent.thread

class ChunkZMQSender(val port: Int, val zContext: ZContext) {

    val thread: Thread = networkThread()

    var running = true

    val bufferLock = Any()

    var currentBuffer: ByteBuffer = MemoryUtil.memAlloc(0)
    val data = ByteArray(CHUNK_SIZE)
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))
    lateinit var router: ZMQ.Socket

    fun close(): Thread {
        running = false
        return thread
    }

    fun sendBuffer(buffer: ByteBuffer) {
        synchronized(bufferLock) {
            currentBuffer = buffer
        }
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
        logger.info("${ChunkZMQSender::class.simpleName} bound to tcp://*:$port")

        while (!Thread.currentThread().isInterrupted && running) {
            //  First frame in each message is the sender identity
            val identity = ZFrame.recvFrame(router) ?: continue

            //  Second frame is "fetch" command
            val command = router.recvStr()
            assert("fetch" == command)

            //  Third frame is chunk offset in file
            val offset = router.recvStr().toInt()
            //  Fourth frame is maximum chunk size
            val chunkSize = router.recvStr().toInt()
            //  Fifth frame is chunk id
            val chunkId = router.recvStr()

            var size: Int
            while (currentBuffer.limit() < 2 && !Thread.currentThread().isInterrupted && running) {
                Thread.sleep(200)
            }
            synchronized(bufferLock) {
                currentBuffer.reset()
                currentBuffer.position(currentBuffer.position() + offset.coerceAtMost(currentBuffer.remaining()))
                size = chunkSize.coerceAtMost(currentBuffer.remaining())
                currentBuffer.get(data, 0, size)
            }

            //  Send resulting chunk to client
            val chunk = ZFrame(data.copyOf(if (size <= 0) 1 else size))
            identity.sendAndDestroy(router, ZMQ.SNDMORE)
            router.sendMore(chunkId)
            chunk.sendAndDestroy(router)
        }
        router.linger = 0
        router.close()
    }
}
