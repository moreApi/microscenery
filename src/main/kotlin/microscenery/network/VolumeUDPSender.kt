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
//        println("send data")
        var index: UInt = 0u
        while (buffer.remaining() >= payloadSize) {
//            if (index % 10u == 0u)
//                println("sending $index")
            val frag = VolumeFragment.fromBuffer(index++, buffer, payloadSize)
            frag.send()
            buffer.position(buffer.position() + payloadSize)
        }
    }
}


fun main() {

    val dummyData = MemoryUtil.memAlloc(166*1000 * Short.SIZE_BYTES )
    dummyData.rewind()

    val server = VolumeUDPSender()
    val receiver = VolumeUDPReceiver()
    thread { receiver.startReceiving() }
    Thread.sleep(1500)

    val t = System.currentTimeMillis()
//    while (true) {
    for (i in 0 until 500){
        println("Sending slice $i")
        server.sendBuffer(dummyData)
        dummyData.rewind()
    }
    println("delta ${System.currentTimeMillis()-t}")
    receiver.running = false

}