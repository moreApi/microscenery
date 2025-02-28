package network
import microscenery.network.BiggishDataClient
import microscenery.network.BiggishDataServer
import microscenery.network.CHUNK_SIZE
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

    private val storageSize = 50 * 1024 * 1024


    fun init(port: Int = 4400, storage: SliceStorage = SliceStorage(storageSize)) {
        zContext = ZContext()
        zContext.linger = 0
        this.storage = storage
        server = BiggishDataServer(port, storage, zContext)
        client = BiggishDataClient(zContext, port)
        Thread.sleep(500)
    }

    /**
     * JUnit can't handle kotlin default parameters :(
     */
    @BeforeEach
    fun init2() = init()

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
    fun tooLarge() {
        storage.addSlice(1, ByteBuffer.wrap(ByteArray(storageSize + 1) { it.toByte() }))
        assertNull(storage.getSlice(1))
    }


    @Test
    fun TenMbTimed() {
        val dataSize = 10 * 1024 * 1024

        storage.addSlice(1, ByteBuffer.wrap(ByteArray(dataSize) { (it + 10).toByte() }))

        val start = System.currentTimeMillis()
        assert(client.requestSlice(1, dataSize))


        val slice = client.outputQueue.poll(20000, TimeUnit.MILLISECONDS)
        assertNotNull(slice)
        val end = System.currentTimeMillis()
        println("Sending 10 mb took ${(end-start)/1000f} seconds")

        // check whole slice
        val buffer = MemoryUtil.memAlloc(slice.size)
        slice.chunks.forEach {
            buffer.put(it.value)
        }
        buffer.flip()

        for (i in 0 until dataSize) {
            val byte = buffer.get()
            assertEquals((i + 10).toByte(), byte, "at index $i")
        }
        assert(end - start < 1000)
    }

    @Test
    fun multiChunkSlice() {
        val dataSize = CHUNK_SIZE * 2 + 5
        //increase storage size
        reset()
        init(4400,SliceStorage(CHUNK_SIZE * 3))

        storage.addSlice(1, ByteBuffer.wrap(ByteArray(dataSize) { (it + 10).toByte() }))
        assert(client.requestSlice(1, dataSize))
        val slice = client.outputQueue.poll(10000, TimeUnit.MILLISECONDS)
        assertNotNull(slice)

        // check whole slice
        val buffer = MemoryUtil.memAlloc(slice.size)
        slice.chunks.forEach {
            buffer.put(it.value)
        }
        buffer.flip()

        for (i in 0 until dataSize) {
            val byte = buffer.get()
            assertEquals((i + 10).toByte(), byte, "at index $i")
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
    fun sliceNotAvailable() {
        client.requestSlice(55, 100)
        // can't really check if it handles it correctly because the interesting properties are private.
        // But if this works.
        simple()
        // it seems not to hang
    }

    @Test
    fun short() {
        val size = 2000
        val bBuffer = MemoryUtil.memAlloc(size * 2)
        val sBuffer = bBuffer.asShortBuffer()

        for (i in (0).toUShort() until size.toUShort()) {
            val s = (UShort.MAX_VALUE / size.toUShort()) * i
            sBuffer.put(s.toShort())
        }

        storage.addSlice(1, bBuffer)
        assert(client.requestSlice(1, size * 2))
        val slice = client.outputQueue.poll(10000, TimeUnit.MILLISECONDS)
        assertNotNull(slice)


        bBuffer.rewind()

        slice.chunks.firstEntry().value.forEach { byte ->
            assertEquals(bBuffer.get(), byte)
        }

        MemoryUtil.memFree(bBuffer)
    }


}