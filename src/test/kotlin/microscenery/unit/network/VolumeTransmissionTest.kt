package microscenery.unit.network

import microscenery.network.VolumeReceiver
import microscenery.network.VolumeSender
import org.junit.jupiter.api.Test
import org.lwjgl.system.MemoryUtil
import kotlin.concurrent.thread
import kotlin.math.pow
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class VolumeTransmissionTest {

    @Test
    fun notReusingBuffer() {
        val connections = 10
        val basePort = 4400

        val dummyData = MemoryUtil.memAlloc(166 * 10.0.pow(6.0).toInt())
        dummyData.rewind()

        val receiver = VolumeReceiver(dummyData.capacity(), connections, basePort, false)
        val sender = VolumeSender(connections, basePort)

        Thread.sleep(1500)

        val t = System.currentTimeMillis()
        sender.sendVolume(dummyData)
        sender.dummyFinish()
        val result = receiver.getVolume(5000)
        assertNotNull(result)

        println("delta ${System.currentTimeMillis() - t}")


        sender.close()
        receiver.close()
    }

    @Test
    fun reusingBuffer() {
        val connections = 10
        val basePort = 4400

        val dummyData = MemoryUtil.memAlloc(166 * 10.0.pow(6.0).toInt())
        for (x in 1..6546) {
            dummyData.put(dummyData.position().toByte())
        }
        dummyData.rewind()

        val receiver = VolumeReceiver(dummyData.capacity(), connections, basePort, true)
        val sender = VolumeSender(connections, basePort)

        Thread.sleep(1500)

        val t = System.currentTimeMillis()

        sender.sendVolume(dummyData)
        sender.dummyFinish()
        val result = receiver.getVolume(5000)
        assertNotNull(result)

        val delta = System.currentTimeMillis() - t

        sender.close()
        receiver.close()

        val through = (dummyData.capacity() / delta) / 1000
        println("delta ${delta} throughput ${through} mByte/Sec")
    }

    @Test
    fun reusingBufferMultipleVolumes() {
        val connections = 10
        val basePort = 4400
        val repeats = 10

        val dummyData = MemoryUtil.memAlloc(166 * 10.0.pow(6.0).toInt())
        for (x in 1..6546) {
            dummyData.put(x.toByte())
        }
        dummyData.rewind()

        val receiver = VolumeReceiver(dummyData.capacity(), connections, basePort, true)
        val sender = VolumeSender(connections, basePort)

        Thread.sleep(1500)

        val t = System.currentTimeMillis()

        val t1 = thread {
            for (x in 1..repeats) {
                println("dummy " + x)
                dummyData.rewind()
                sender.sendVolume(dummyData)
            }
            sender.dummyFinish()
        }

        val t2 = thread {
            for (x in 1..repeats) {
                val result = receiver.getVolume(5000)
                assertNotNull(result)
                println("Got volume " + x)
                result.rewind()
                for (i in 1..6546) {
                    assertEquals(i.toByte(), result.get())
                }
                result.rewind()
            }
        }
        t1.join()
        t2.join()

        val delta = System.currentTimeMillis() - t

        sender.close()
        receiver.close()

        val through = ((dummyData.capacity() * repeats.toLong()) / delta) / 1000
        println("delta ${delta} throughput ${through} mByte/Sec")
    }

}