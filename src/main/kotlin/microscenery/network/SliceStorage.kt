package microscenery.network

import microscenery.MicroscenerySettings
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentSkipListMap


/**
 * Stores bytebuffers and takes up to [maxStorageSize] bytes.
 * If more data is added the oldest data is discarded until enough space is freed.
 */
class SliceStorage(val maxStorageSize: Int = MicroscenerySettings.get("Network.DataStorageSizeMb", 2) * 1024 * 1024) {

    private var currentlyStoredBytes = 0

    private var storage = mapOf<Int, ByteBuffer>()
    private val sliceTimestamps = ConcurrentSkipListMap<Long, Int>()

    /**
     * Not thread save
     */
    fun addSlice(id: Int, data: ByteBuffer) {
        while (currentlyStoredBytes + data.capacity() > maxStorageSize) {
            val toBeDropped = sliceTimestamps.firstEntry()
            sliceTimestamps.remove(toBeDropped.key)
            val sliceTBDid = toBeDropped.value
            // seems like the buffer does not need to be freed since we are not using direct buffers
            val sliceTBDdata = storage.get(sliceTBDid)
                ?: throw IllegalStateException("Wanted to drop slice $sliceTBDid but data was not to be found.")
            storage = storage.minus(sliceTBDid)
            currentlyStoredBytes -= sliceTBDdata.capacity()
        }
        storage = storage.plus(id to data)
        currentlyStoredBytes += data.capacity()
        sliceTimestamps[System.currentTimeMillis()] = id
    }

    fun getSlice(id: Int): ByteBuffer? = storage[id]

    fun newSlice(size: Int): ByteBuffer = ByteBuffer.allocateDirect(size)

}