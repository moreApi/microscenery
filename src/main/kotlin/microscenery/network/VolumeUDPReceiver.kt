package microscenery.network

import org.lwjgl.system.MemoryUtil
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.nio.ByteBuffer
import kotlin.concurrent.thread

val payloadSize = 15 * 15 * Short.SIZE_BYTES
val packetSize = payloadSize + Int.SIZE_BYTES

class VolumeUDPReceiver {
    val socket = DatagramSocket(4445);

    val buffer = ByteArray(packetSize)

    init {
    }

    fun startReceiving() {
        thread {
            println("Start receiver")
            while (true) {
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)

                val buf = ByteBuffer.wrap(buffer)

                val index = ((buf.get().toUInt() and 0xFFu) shl 24) or
                        ((buf.get().toUInt() and 0xFFu) shl 16) or
                        ((buf.get().toUInt() and 0xFFu) shl 8) or
                        (buf.get().toUInt() and 0xFFu)
                println(index)
            }
        }
    }

}