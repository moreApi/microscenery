package microscenery.unit.network

import microscenery.network.ChunkUDPReceiver
import microscenery.network.ChunkUDPSender
import microscenery.network.FRAGMENT_PAYLOAD_SIZE
import org.junit.Test
import org.lwjgl.system.MemoryUtil
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ChunkUPDTransmissionTest {

    @Test
    fun simple(){
        val basePort = 4400
        val sender =  ChunkUDPSender(basePort)
        val receiver =  ChunkUDPReceiver(basePort,0u)
        val st = sender.startSending()
        val rt = receiver.startReceiving()

        val dummyData = MemoryUtil.memAlloc(FRAGMENT_PAYLOAD_SIZE*2)
        for (x in 1..FRAGMENT_PAYLOAD_SIZE*2)
            dummyData.put(x.toByte())

        dummyData.rewind()
        sender.sendBuffer(dummyData)
        //send twice to trigger new chunk output
        dummyData.rewind()
        sender.sendBuffer(dummyData)

        val result = receiver.outputQueue.poll(5, TimeUnit.SECONDS)?.poll()
        assertNotNull(result)

        for (x in 1..5)
            assert(result.data.get() == x.toByte())

        sender.running = false
        receiver.running = false
        st.join()
        rt.join()
    }

    @Test
    fun multiple(){
        val basePort = 4400
        val sender =  ChunkUDPSender(basePort)
        val receiver =  ChunkUDPReceiver(basePort,0u)
        val st = sender.startSending()
        val rt = receiver.startReceiving()

        val dummyData = MemoryUtil.memAlloc(FRAGMENT_PAYLOAD_SIZE*2)
        for (x in 1..FRAGMENT_PAYLOAD_SIZE*2)
            dummyData.put(x.toByte())

        dummyData.rewind()
        sender.sendBuffer(dummyData)

        for (i in 1 .. 10) {
            dummyData.rewind()
            sender.sendBuffer(dummyData)

            val result = receiver.outputQueue.poll(5, TimeUnit.SECONDS)?.poll()
            assertNotNull(result)

            for (x in 1..5)
                assertEquals(result.data.get(), x.toByte())
        }
        sender.running = false
        receiver.running = false
        st.join()
        rt.join()
    }


    @Test
    fun notFullLastFragment(){
        val basePort = 4400
        val sender =  ChunkUDPSender(basePort)
        val receiver =  ChunkUDPReceiver(basePort,0u)
        val st = sender.startSending()
        val rt = receiver.startReceiving()


        val unalignedData = MemoryUtil.memAlloc(FRAGMENT_PAYLOAD_SIZE+5)
        for (x in 1..FRAGMENT_PAYLOAD_SIZE+5)
            unalignedData.put(1)
        unalignedData.rewind()
        sender.sendBuffer(unalignedData)

        // trigger new chunk output
        val dummyData = MemoryUtil.memAlloc(FRAGMENT_PAYLOAD_SIZE*2)
        for (x in 1..FRAGMENT_PAYLOAD_SIZE*2)
            dummyData.put(x.toByte())

        dummyData.rewind()
        sender.sendBuffer(dummyData)


        val resultQueue = receiver.outputQueue.poll(5, TimeUnit.SECONDS)
        //skip first fragment
        resultQueue?.poll()
        val result = resultQueue?.poll()
        assertNotNull(result)


        for (x in 1..5)
            assertEquals(1.toByte(), result.data.get())
        assertEquals((1).toByte(), result.data.get())

        sender.running = false
        receiver.running = false
        st.join()
        rt.join()
    }


}