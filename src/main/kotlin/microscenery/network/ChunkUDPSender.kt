package microscenery.network

import org.lwjgl.system.MemoryUtil
import java.net.*
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


class ChunkUDPSender(val port: Int) {

    val socket = DatagramSocket()
    val address = InetAddress.getByName("localhost")
    val packetBuffer = ByteBuffer.wrap(ByteArray(packetSize + 1))

    val inputQueue = ArrayBlockingQueue<ByteBuffer>(2)
    var running = true

    init {
        println("init server pointing at $port")
    }

    fun startSending() = thread {
            while (running) {
                sendBuffer(inputQueue.poll(2, TimeUnit.SECONDS) ?: continue)
            }
        }

    fun sendBuffer(buffer: ByteBuffer) {
//        println("send data")
        var index: UInt = 0u
        while (buffer.remaining() >= FRAGMENT_PAYLOAD_SIZE) {
            val frag = VolumeFragment.fromBuffer(index++, buffer, FRAGMENT_PAYLOAD_SIZE)
            frag.send()
            buffer.position(buffer.position() + FRAGMENT_PAYLOAD_SIZE)
        }
        if (buffer.hasRemaining()) {
            val frag = VolumeFragment.fromBuffer(index, buffer, buffer.remaining())
            frag.send()
        }
    }

    fun VolumeFragment.send() {
        data.reset()
        packetBuffer.clear()

        packetBuffer.putInt(this.id.toInt())
        packetBuffer.put(this.data)

        val packet = DatagramPacket(packetBuffer.array(), packetBuffer.capacity(), address, port)
        socket.send(packet)
    }
}


fun main() {
    val connections = 30
    val basePort = 4400
    val senders = (basePort until basePort+connections).map { ChunkUDPSender(it) }.toList()
    val receivers = (basePort until basePort+connections).map {
        val a = ChunkUDPReceiver(it)
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
        senders.forEach {
            it.startSending()
            it.inputQueue.put(dummyData.duplicate())
        }
    }
    println("delta ${System.currentTimeMillis()-t}")
    receivers.forEach {
        it.running = false
    }
    senders.forEach {
        it.running = false
    }

}