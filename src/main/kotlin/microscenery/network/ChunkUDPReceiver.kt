package microscenery.network

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread

const val FRAGMENT_PAYLOAD_SIZE = 15 * 15 * Short.SIZE_BYTES
const val packetSize = FRAGMENT_PAYLOAD_SIZE + Int.SIZE_BYTES

class ChunkUDPReceiver(val port: Int, val newChunkThreshold: UInt = 5u) {
    val socket = DatagramSocket(port)

    var running = true
    val outputQueue = ArrayBlockingQueue<PriorityQueue<VolumeFragment>>(2)

    var fragments = PriorityQueue<VolumeFragment>(compareBy { it.id })
    var lastIndex = 0u

    var thread: Thread? = null

    init {
    }

    fun startReceiving(): Thread {
        thread =  thread {
            println("Start receiver at $port")
            socket.soTimeout = 2000
            while (running) {
                try {
                    val buffer = ByteArray(packetSize)
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)

                    val buf = ByteBuffer.wrap(buffer)

                    val index = ((buf.get().toUInt() and 0xFFu) shl 24) or
                            ((buf.get().toUInt() and 0xFFu) shl 16) or
                            ((buf.get().toUInt() and 0xFFu) shl 8) or
                            (buf.get().toUInt() and 0xFFu)

                    if (lastIndex+newChunkThreshold > index){
                        // start new slice
                        if (outputQueue.remainingCapacity() != 0)
                            outputQueue.put(fragments)
                        fragments = PriorityQueue<VolumeFragment>(compareBy { it.id })
                    }
                    fragments.add(VolumeFragment(index, buf.mark()))

                    lastIndex = index
                } catch (timeout: java.net.SocketTimeoutException) {
    //                    println("got Timeout")
                }
            }
        }
        return thread!!
    }

    fun close(wait: Boolean = false){
        running = false
        socket.close()
        if (wait) thread?.join()
    }
}