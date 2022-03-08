package microscenery.network

import org.lwjgl.system.MemoryUtil
import java.net.*
import java.nio.ByteBuffer
import kotlin.concurrent.thread


class VolumeUDPSender {

    val socket = DatagramSocket()
    val address = InetAddress.getByName("localhost")
    val packetBuffer = ByteBuffer.wrap(ByteArray(packetSize + 1))

    init {
        println("init server")
    }

    fun VolumeFragment.send() {
        data.reset()
        packetBuffer.rewind()

        packetBuffer.putInt(this.id.toInt())
        packetBuffer.put(this.data)

        val packet = DatagramPacket(packetBuffer.array(), packetBuffer.capacity(), address, 4445)
        socket.send(packet)
    }

    fun sendBuffer(buffer: ByteBuffer) {
        println("send data")
        var index: UInt = 0u
        while (buffer.remaining() >= payloadSize) {
            if (index % 10u == 0u)
                println("sending $index")
            val frag = VolumeFragment.fromBuffer(index++, buffer, payloadSize)
            frag.send()
            buffer.position(buffer.position() + payloadSize)
        }
    }
}


fun main() {

    val dummyData = MemoryUtil.memAlloc(15 * 15 * Short.SIZE_BYTES * 100)
    dummyData.rewind()

    val server = VolumeUDPSender()
    val receiver = VolumeUDPReceiver()
    thread { receiver.startReceiving() }
    Thread.sleep(100)

    while (true) {
        server.sendBuffer(dummyData)
        dummyData.rewind()
        Thread.sleep(500)
    }

}