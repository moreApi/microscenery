package microscenery.unit.network

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import microscenery.network.VolumeReceiver
import microscenery.network.VolumeSender
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.lwjgl.system.MemoryUtil
import org.zeromq.ZContext
import kotlin.math.pow
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class VolumeTransmissionTest {

    var ctx = ZContext()

    @AfterEach
    fun reset() {
        ctx.linger =  0
        ctx.destroy()
        ctx = ZContext()
        ctx.linger =  0
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

        val receiver = VolumeReceiver(true, zContext = ctx, dummyData.capacity(), connections, basePort)
        val sender = VolumeSender(ctx, connections, basePort)

        Thread.sleep(1500)

        val t = System.currentTimeMillis()

        sender.sendVolume(dummyData)
        val result = receiver.getVolume(5000)
        assertNotNull(result)

        val delta = System.currentTimeMillis() - t

        (receiver.close() + sender.close()).forEach { it.join() }

        val through = (dummyData.capacity() / delta) / 1000
        println("delta ${delta} throughput ${through} mByte/Sec")
    }

    @Test
    fun notReusingBuffer() {
        val connections = 10
        val basePort = 4400

        val dummyData = MemoryUtil.memAlloc(166 * 10.0.pow(6.0).toInt())
        dummyData.rewind()

        val receiver = VolumeReceiver(false, zContext = ctx, dummyData.capacity(), connections, basePort)
        val sender = VolumeSender(ctx, connections, basePort)

        Thread.sleep(1500)

        val t = System.currentTimeMillis()
        sender.sendVolume(dummyData)
        val result = receiver.getVolume(5000)
        assertNotNull(result)

        val delta = System.currentTimeMillis() - t
        val through = (dummyData.capacity() / delta) / 1000
        println("delta ${delta} throughput ${through} mByte/Sec")

        (receiver.close() + sender.close()).forEach { it.join() }
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

        val receiver = VolumeReceiver(true, zContext = ctx, dummyData.capacity(), connections, basePort)
        val sender = VolumeSender(zContext = ctx, connections, basePort)

        Thread.sleep(1500)

        val t = System.currentTimeMillis()

        runBlocking {

            val t1 = launch {
                for (x in 1..repeats) {
                    println("dummy " + x)
                    dummyData.rewind()
                    sender.sendVolume(dummyData)
                }
            }

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

            t1.join()
        }

        val delta = System.currentTimeMillis() - t

        (receiver.close() + sender.close()).forEach { it.join() }

        val through = ((dummyData.capacity() * repeats.toLong()) / delta) / 1000
        println("delta ${delta} throughput ${through} mByte/Sec")
    }

}