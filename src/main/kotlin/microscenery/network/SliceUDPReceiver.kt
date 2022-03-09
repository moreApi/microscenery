package microscenery.network

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread

const val PAYLOAD_SIZE = 15 * 15 * Short.SIZE_BYTES
const val packetSize = PAYLOAD_SIZE + Int.SIZE_BYTES


class SliceUDPReceiver(val port: Int) {
    val socket = DatagramSocket(port);


    var running = true

    val outputQueue = ArrayBlockingQueue<PriorityQueue<VolumeFragment>>(2)

    var fragments = PriorityQueue<VolumeFragment>(compareBy { it.id })
    var lastIndex = 0u

    init {
    }

    fun startReceiving() {
        thread {
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
                    val fragment = VolumeFragment(index, buf.mark())

                    if (lastIndex+5u > index){
                        // start new slice
                        if (outputQueue.remainingCapacity() != 0)
                            outputQueue.put(fragments)
                        var fragments = PriorityQueue<VolumeFragment>(compareBy { it.id })
                    }

                } catch (timeout: java.net.SocketTimeoutException) {
//                    println("got Timeout")
                }
            }
        }
    }
}