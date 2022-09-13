package microscenery

import graphics.scenery.utils.RingBuffer
import microscenery.network.VolumeSender
import mmcorej.CMMCore
import org.lwjgl.system.MemoryUtil
import org.zeromq.ZContext
import java.nio.ByteBuffer
import kotlin.concurrent.thread

val zContext = ZContext()

//TODO is this clas needed?
class MMVolumeSender(core: CMMCore? = null){

    val mmConnection = MMConnection(core_ = core)

    val volumeSize = mmConnection.width * mmConnection.height //* mmConnection.steps * Short.SIZE_BYTES
    val volumeSender = VolumeSender(zContext)

    val timeBetweenUpdates = 1000

    val volumeBuffers = RingBuffer<ByteBuffer>(2, default = {
        MemoryUtil.memAlloc(4)//(mmConnection.width * mmConnection.height * mmConnection.steps * Short.SIZE_BYTES))
    })
    var time = 0L

    var running = true


    init {
        //println("Start MM Sender with  ${mmConnection.width}x${mmConnection.height}x${mmConnection.steps}xShort = $volumeSize bytes at port ${volumeSender.basePort}")

        // imaging and sending thread
        thread {
            Thread.sleep(200)
            while (running) {
                //wait at least timeBetweenUpdates
                (System.currentTimeMillis() - time).let {
                    if (it in 1..timeBetweenUpdates)
                        Thread.sleep(timeBetweenUpdates - it)
                }
                time = System.currentTimeMillis()

                val buf = volumeBuffers.get()
                buf.clear()
                //mmConnection.captureStack(buf.asShortBuffer())
                buf.rewind()
                volumeSender.sendVolume(buf)
            }
            println("stopped capturing")
            zContext.destroy()

        }
    }

    fun stop(){
        running = false
    }
}