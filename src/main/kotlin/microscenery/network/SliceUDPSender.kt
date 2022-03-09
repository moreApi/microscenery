package microscenery.network

import org.lwjgl.system.MemoryUtil
import java.net.*
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


class SliceUDPSender(val port: Int) {

    val socket = DatagramSocket()
    val address = InetAddress.getByName("localhost")
    val packetBuffer = ByteBuffer.wrap(ByteArray(packetSize + 1))

    val inputQueue = ArrayBlockingQueue<ByteBuffer>(2)
    var running = true

    init {
        println("init server pointing at $port")

        thread {
            while (running) {
                sendBuffer(inputQueue.poll(2000, TimeUnit.MICROSECONDS) ?: continue)
            }
        }
    }

    fun VolumeFragment.send() {
        data.reset()
        packetBuffer.rewind()

        packetBuffer.putInt(this.id.toInt())
        packetBuffer.put(this.data)

        val packet = DatagramPacket(packetBuffer.array(), packetBuffer.capacity(), address, port)
        socket.send(packet)
    }

    fun sendBuffer(buffer: ByteBuffer) {
//        println("send data")
        var index: UInt = 0u
        while (buffer.remaining() >= PAYLOAD_SIZE) {
//            if (index % 10u == 0u)
//                println("sending $index")
            val frag = VolumeFragment.fromBuffer(index++, buffer, PAYLOAD_SIZE)
            frag.send()
            buffer.position(buffer.position() + PAYLOAD_SIZE)
        }
    }
}


fun main() {
    val connections = 30
    val basePort = 4400
    val senders = (basePort until basePort+connections).map { SliceUDPSender(it) }.toList()
    val receivers = (basePort until basePort+connections).map {
        val a = SliceUDPReceiver(it)
        thread { a.startReceiving() }
        a
    }.toList()

    val dummyData = MemoryUtil.memAlloc(166*1000 * 10 * Short.SIZE_BYTES )
    dummyData.rewind()

    Thread.sleep(1500)

    val t = System.currentTimeMillis()
//    while (true) {
    for (i in 0 until 500/connections){
        println("Sending slice $i")
        senders.forEach { it.inputQueue.put(dummyData.duplicate()) }
    }
    println("delta ${System.currentTimeMillis()-t}")
    receivers.forEach {
        it.running = false
    }
    senders.forEach {
        it.running = false
    }

}