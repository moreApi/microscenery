package microscenery.unit.network

import microscenery.network.BiggishDataClient
import microscenery.network.BiggishDataServer
import microscenery.network.SliceStorage
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.lwjgl.system.MemoryUtil
import org.zeromq.ZContext
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BiggishDataTransmissionTest {


    lateinit var zContext: ZContext
    lateinit var storage: SliceStorage
    lateinit var server: BiggishDataServer
    lateinit var client: BiggishDataClient

    @BeforeEach
    fun init() {
        zContext = ZContext()
        zContext.linger = 0
        storage = SliceStorage(50 * 1024 * 1024)
        server = BiggishDataServer(4400, storage, zContext)
        client = BiggishDataClient(zContext, 4400)
        Thread.sleep(500)
    }

    @AfterEach
    fun reset() {
        val t = server.close()
        client.close().join(2000)
        t.join(2000)
        zContext.linger = 0
        zContext.destroy()
    }

    @Test
    fun simple() {
        storage.addSlice(1, ByteBuffer.wrap(ByteArray(100) { it.toByte() }))
        assert(client.requestSlice(1, 100))
        val slice = client.outputQueue.poll(10000, TimeUnit.MILLISECONDS)
        assertNotNull(slice)
        slice.chunks.firstEntry().value.forEachIndexed { index, byte ->
            assertEquals(index.toByte(), byte)
        }
    }

    @Test
    fun twice() {
        storage.addSlice(1, ByteBuffer.wrap(ByteArray(100) { it.toByte() }))
        storage.addSlice(2, ByteBuffer.wrap(ByteArray(100) { it.toByte() }))
        assert(client.requestSlice(1, 100))
        assert(client.requestSlice(2, 100))
        val slice = client.outputQueue.poll(10000, TimeUnit.MILLISECONDS)
        assertNotNull(slice)
        slice.chunks.firstEntry().value.forEachIndexed { index, byte ->
            assertEquals(index.toByte(), byte)
        }
        val slice2 = client.outputQueue.poll(10000, TimeUnit.MILLISECONDS)
        assertNotNull(slice2)
        slice2.chunks.firstEntry().value.forEachIndexed { index, byte ->
            assertEquals(index.toByte(), byte)
        }
    }

    @Test
    fun storageOverflow() {
        storage.addSlice(1, MemoryUtil.memAlloc(26 * 1024 * 1024))
        storage.addSlice(2, MemoryUtil.memAlloc(26 * 1024 * 1024))
        storage.addSlice(3, MemoryUtil.memAlloc(26 * 1024 * 1024))

        assertNull(storage.getSlice(1))
    }

    @Test
    fun sliceNotAvailable(){
        client.requestSlice(55,100)
        // can't really check if it handles it correctly because the interesting properties are private.
        // But if this works.
        simple()
        // it seems not to hang
    }


}