package microscenery.network

import fromScenery.lazyLogger
import microscenery.MicroscenerySettings
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentSkipListMap


/**
 * Stores bytebuffers and takes up to [maxStorageSize] bytes.
 * If more data is added the oldest data is discarded until enough space is freed.
 *
 * Default is 2mb.
 */
class SliceStorage(val maxStorageSize: Int = MicroscenerySettings.get("Network.DataStorageSizeMb", 2) * 1024 * 1024) {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private var currentlyStoredBytes = 0

    private var storage = mapOf<Int, ByteBuffer>()
    private val sliceTimestamps = ConcurrentSkipListMap<Long, Int>()

    /**
     * Not thread save
     * Transfers the memory responsibility for [data] to this class.
     * Slice storage works on only on whole buffers. Position and limit are ignored!
     */
    fun addSlice(id: Int, data: ByteBuffer) {
        if (maxStorageSize < data.capacity()) {
            logger.error(
                "slice with id $id is could not be put in server storage since it exceeds the max storage " +
                        "capacity $maxStorageSize < ${data.capacity()}"
            )
            return
        }

        while (currentlyStoredBytes + data.capacity() > maxStorageSize) {
            val toBeDropped = sliceTimestamps.firstEntry()
            sliceTimestamps.remove(toBeDropped.key)
            // ToBeDropped (TBD)
            val sliceTBDid = toBeDropped.value
            val sliceTBDdata = storage.get(sliceTBDid)
                ?: throw IllegalStateException("Wanted to drop slice $sliceTBDid but data was not to be found.")
            storage = storage.minus(sliceTBDid)
            logger.info("Dropping slice $sliceTBDid to free memory.")
            MemoryUtil.memFree(sliceTBDdata)
            //TODO somehow propagate status of freed buffer
            currentlyStoredBytes -= sliceTBDdata.capacity()
        }
        storage = storage.plus(id to data)
        currentlyStoredBytes += data.capacity()
        sliceTimestamps[System.currentTimeMillis()] = id
    }

    fun getSlice(id: Int): ByteBuffer? = storage[id]

    fun newSlice(size: Int): ByteBuffer = MemoryUtil.memAlloc(size)

}