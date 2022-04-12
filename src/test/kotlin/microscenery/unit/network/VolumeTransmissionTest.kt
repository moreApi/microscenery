package microscenery.unit.network

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import microscenery.network.VolumeReceiver
import microscenery.network.VolumeSender
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.lwjgl.system.MemoryUtil
import org.zeromq.ZContext
import kotlin.math.pow
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class VolumeTransmissionTest {

    //lateinit var ctx : ZContext
    var ctx = ZContext()

    @AfterEach
    fun reset() {
        ctx.linger =  0
        ctx.destroy()
        ctx = ZContext()
        ctx.linger =  0
    }

    @BeforeEach
    fun init(){
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

        val receiver = VolumeReceiver(dummyData.capacity(), connections, basePort, true, zContext = ctx)
        val sender = VolumeSender(connections, basePort, ctx)

        Thread.sleep(1500)

        val t = System.currentTimeMillis()

        sender.sendVolume(dummyData)
        val result = receiver.getVolume(5000)
        assertNotNull(result)

        val delta = System.currentTimeMillis() - t

        sender.close()
        receiver.close()

        val through = (dummyData.capacity() / delta) / 1000
        println("delta ${delta} throughput ${through} mByte/Sec")
    }

    @Test
    fun notReusingBuffer() {
        val connections = 10
        val basePort = 4400

        val dummyData = MemoryUtil.memAlloc(166 * 10.0.pow(6.0).toInt())
        dummyData.rewind()

        val receiver = VolumeReceiver(dummyData.capacity(), connections, basePort, false, zContext = ctx)
        val sender = VolumeSender(connections, basePort, ctx)

        Thread.sleep(1500)

        val t = System.currentTimeMillis()
        sender.sendVolume(dummyData)
        val result = receiver.getVolume(5000)
        assertNotNull(result)

        println("delta ${System.currentTimeMillis() - t}")


        sender.close()
        receiver.close()

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

        val receiver = VolumeReceiver(dummyData.capacity(), connections, basePort, true, zContext = ctx)
        val sender = VolumeSender(connections, basePort, zContext = ctx)

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
            sender.close()
            receiver.close()

            t1.join()
        }

        val delta = System.currentTimeMillis() - t


        val through = ((dummyData.capacity() * repeats.toLong()) / delta) / 1000
        println("delta ${delta} throughput ${through} mByte/Sec")
    }

}