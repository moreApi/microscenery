package microscenery.network

import graphics.scenery.utils.LazyLogger
import org.lwjgl.system.MemoryUtil
import org.zeromq.*
import java.nio.ByteBuffer

class ChunkZMQSender(val port: Int, val zContext: ZContext) {

    private val server = Server(port)
    val thread = ZThread.fork(zContext, server)

    fun close(){
        server.running = false
    }

    fun sendBuffer(buffer: ByteBuffer) {
        synchronized(server.bufferLock) {
            server.currentBuffer = buffer
        }
    }

    // taken from https://zguide.zeromq.org/docs/chapter7/#Transferring-Files
    //  The rest of the code is exactly the same as in model 2, except
    //  that we set the HWM on the server's ROUTER socket to PIPELINE
    //  to act as a sanity check.
    //  The server thread waits for a chunk request from a client,
    //  reads that chunk and sends it back to the client:
    internal class Server(val port: Int) : ZThread.IAttachedRunnable {
        var running = true

        val bufferLock = Any()
        var currentBuffer: ByteBuffer = MemoryUtil.memAlloc(0)
        val data = ByteArray(CHUNK_SIZE + 1)
        private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))
        lateinit var router: ZMQ.Socket

        override fun run(args: Array<Any>, ctx: ZContext, pipe: ZMQ.Socket) {

            router = ctx.createSocket(SocketType.ROUTER)
            router.hwm = PIPELINE * 2
            router.bind("tcp://*:$port")
            logger.info("${ChunkZMQSender::class.simpleName} bound to tcp://*:$port")

            while (!Thread.currentThread().isInterrupted && running) {
                //  First frame in each message is the sender identity
                val identity = ZFrame.recvFrame(router)
                if (identity.data == null) break //  Shutting down, quit

                //  Second frame is "fetch" command
                val command = router.recvStr()
                assert("fetch" == command)

                //  Third frame is chunk offset in file
                val offset = router.recvStr().toInt()
                //  Fourth frame is maximum chunk size
                val chunkSize = router.recvStr().toInt()
                //  Fifth frame is chunk id
                val chunkId = router.recvStr().toByte()

                var size: Int
                while (currentBuffer.limit() < 2 && !Thread.currentThread().isInterrupted && running) {
                    Thread.sleep(200)
                }
                synchronized(bufferLock) {
                    currentBuffer.reset()
                    currentBuffer.position(currentBuffer.position() + offset.coerceAtMost(currentBuffer.remaining()))
                    size = chunkSize.coerceAtMost(currentBuffer.remaining())
                    data[0] = chunkId
                    currentBuffer.get(data, 1, size)
                }

                //  Send resulting chunk to client
                val chunk = ZFrame(data.copyOf(if (size < 0) 1 else size + 1))
                identity.sendAndDestroy(router, ZMQ.SNDMORE)
                chunk.sendAndDestroy(router)
            }
            router.linger = 0
            router.close()
            pipe.send("OK")
        }
    }
}