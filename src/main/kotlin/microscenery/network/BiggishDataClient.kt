package microscenery.network

import graphics.scenery.utils.LazyLogger
import me.jancasus.microscenery.network.v2.ReplyHeaderSliceChunk
import me.jancasus.microscenery.network.v2.RequestSliceChunk
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZFrame
import org.zeromq.ZMQ
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

internal const val PIPELINE = 10
internal const val CHUNK_SIZE = 250000

/**
 * Client that request and receives data from [BiggishDataServer].
 *
 * Takes request via [requestSlice] asynchronously.
 * Fully received slices will be put in [outputQueue]. Blocks if the queue is full.
 *
 * Uses a credit system to avoid overflowing the transmission medium.
 */
class BiggishDataClient(val zContext: ZContext, val port: Int, val host: String = "localhost") {
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

    val thread: Thread = networkThread()
    lateinit var dealer: ZMQ.Socket

    val outputQueue = ArrayBlockingQueue<SliceChunkCollector>(10)
    private val requestQueue = ArrayBlockingQueue<SliceChunkCollector>(10)

    var running = true

    private var openSlices = emptyList<SliceChunkCollector>()

    //  Up to this many chunks in transit
    var credit = PIPELINE

    fun close(): Thread {
        running = false
        return thread
    }

    fun requestSlice(id: Int, size: Int, waitOnFull: Boolean = false): Boolean {
        val element = SliceChunkCollector(id, size)
        return if (waitOnFull) {
            requestQueue.offer(element, 5000, TimeUnit.MILLISECONDS)
        } else {
            requestQueue.offer(element)
        }
    }

    // taken from https://zguide.zeromq.org/docs/chapter7/#Transferring-Files
    private fun networkThread() = thread {
        dealer = zContext.createSocket(SocketType.DEALER)
        dealer.connect("tcp://$host:$port")
        dealer.receiveTimeOut = 200

        logger.info("${BiggishDataClient::class.simpleName} connected to tcp://$host:$port")


        while (running && !Thread.currentThread().isInterrupted) {
            if (outputQueue.remainingCapacity() == 0) {
                Thread.sleep(200)
                continue
            }

            while (credit > 0) {
                val openSlice = openSlices.firstOrNull { it.requestedChunks < it.numberOfChunks }
                when {
                    openSlice != null -> {
                        //get next slice requests params
                        openSlice.requestedChunks += 1
                        val reqBuilder = RequestSliceChunk.newBuilder()
                        reqBuilder.sliceId = openSlice.id

                        reqBuilder.chunkSize = if (openSlice.requestedChunks == openSlice.numberOfChunks) {
                            openSlice.size.rem(CHUNK_SIZE)
                        } else {
                            CHUNK_SIZE
                        }

                        reqBuilder.offset = (openSlice.requestedChunks - 1) * CHUNK_SIZE
                        dealer.send(reqBuilder.build().toByteArray())
                        credit--
                    }
                    requestQueue.isNotEmpty() -> {
                        openSlices = openSlices + requestQueue.poll()
                    }
                    else -> {
                        break // maybe some data arrived
                    }
                }
            }
            val reply = ReplyHeaderSliceChunk.parseFrom(dealer.recv() ?: continue)
            credit++

            val sliceCollector = openSlices.firstOrNull { it.id == reply.sliceId } ?: continue

            if (!reply.sliceAvailable) {
                openSlices = openSlices.minus(sliceCollector)
                continue
            }

            val chunk = ZFrame.recvFrame(dealer)
            sliceCollector.chunks[reply.offset] = chunk.data

            if (sliceCollector.isFull()) {
                openSlices = openSlices.minus(sliceCollector)
                outputQueue.put(sliceCollector)
            }
        }
        dealer.linger = 0
        dealer.close()
    }
}

class SliceChunkCollector(val id: Int, val size: Int) {
    val numberOfChunks = if (size < CHUNK_SIZE) 1 else {
        size.floorDiv(CHUNK_SIZE) + if (size.rem(CHUNK_SIZE) > 0) 1 else 0
    }
    var requestedChunks = 0

    // key is offset
    val chunks = ConcurrentSkipListMap<Int, ByteArray>()

    fun isFull() = numberOfChunks == chunks.size
}
