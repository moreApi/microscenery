import org.lwjgl.system.MemoryUtil
import org.zeromq.*
import org.zeromq.ZThread.IAttachedRunnable
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*


// taken from https://zguide.zeromq.org/docs/chapter7/#Transferring-Files

//  File Transfer model #3
//
//  In which the client requests each chunk individually, using
//  command pipelining to give us a credit-based flow control.
object Fileio3 {
    private const val PIPELINE = 10
    private const val CHUNK_SIZE = 250000

    //  The main task starts the client and server threads; it's easier
    //  to test this as a single process with threads, than as multiple
    //  processes:
    @JvmStatic
    fun main(args: Array<String>) {
        val volumeSize = 1000*1000*116
        val dummyData = MemoryUtil.memAlloc(volumeSize)
        for (x in 1 .. 100)
            dummyData.put(x.toByte())
        dummyData.rewind()

        val ctx = ZContext()
        val t = System.currentTimeMillis()
        //  Start child threads
        ZThread.fork(ctx, Server(dummyData))
        val client = ZThread.fork(ctx, Client())
        //  Loop until client tells us it's done
        client.recvStr()
        val delta = System.currentTimeMillis() - t
        println("took $delta ms ${(volumeSize/delta)/1000} mByte/sec")
        //  Kill server thread
        ctx.destroy()
    }

    internal class Client : IAttachedRunnable {
        override fun run(args: Array<Any>, ctx: ZContext, pipe: ZMQ.Socket) {
            val dealer = ctx.createSocket(SocketType.DEALER)
            dealer.connect("tcp://127.0.0.1:6000")

            //  Up to this many chunks in transit
            var credit = PIPELINE
            var total = 0 //  Total bytes received
            var chunks = 0 //  Total chunks received
            var offset = 0 //  Offset of next chunk request
            while (true) {
                while (credit > 0) {
                    //  Ask for next chunk
                    dealer.sendMore("fetch")
                    dealer.sendMore(offset.toString())
                    dealer.send(CHUNK_SIZE.toString())
                    offset += CHUNK_SIZE
                    credit--
                }
                val chunk = ZFrame.recvFrame(dealer)
                if (chunk.data == null) break //  Shutting down, quit
                chunks++
                credit++
                val size = chunk.size()
                chunk.destroy()
                total += size
                if (size < CHUNK_SIZE) break //  Last chunk received; exit
            }
            System.out.printf("%d chunks received, %d bytes\n", chunks, total)
            pipe.send("OK")
        }
    }

    //  The rest of the code is exactly the same as in model 2, except
    //  that we set the HWM on the server's ROUTER socket to PIPELINE
    //  to act as a sanity check.
    //  The server thread waits for a chunk request from a client,
    //  reads that chunk and sends it back to the client:
    internal class Server(val dummyData: ByteBuffer) : IAttachedRunnable {
        override fun run(args: Array<Any>, ctx: ZContext, pipe: ZMQ.Socket) {


            val router = ctx.createSocket(SocketType.ROUTER)
            router.hwm = PIPELINE * 2
            router.bind("tcp://*:6000")
            while (!Thread.currentThread().isInterrupted) {
                //  First frame in each message is the sender identity
                val identity = ZFrame.recvFrame(router)
                if (identity.data == null) break //  Shutting down, quit

                //  Second frame is "fetch" command
                val command = router.recvStr()
                assert("fetch" == command)

                //  Third frame is chunk offset in file
                val offset = router.recvStr().toInt().coerceAtMost(dummyData.capacity()-1)

                //  Fourth frame is maximum chunk size
                val chunkSize = router.recvStr().toInt()

                //  Read chunk of data from file
                val data = ByteArray(CHUNK_SIZE)
                dummyData.position(offset)
                val size = chunkSize.coerceAtMost(dummyData.remaining())
                dummyData.get(data,0,size)

                //  Send resulting chunk to client
                val chunk = ZFrame(Arrays.copyOf(data, if (size < 0) 0 else size))
                identity.sendAndDestroy(router, ZMQ.SNDMORE)
                chunk.sendAndDestroy(router)
            }
        }
    }
}