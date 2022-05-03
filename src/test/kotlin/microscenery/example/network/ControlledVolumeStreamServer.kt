package microscenery.example.network

import getPropertyInt
import graphics.scenery.utils.LazyLogger
import graphics.scenery.utils.RingBuffer
import microscenery.MMConnection
import microscenery.network.*
import mmcorej.CMMCore
import org.joml.Vector3i
import org.lwjgl.system.MemoryUtil
import org.zeromq.ZContext
import java.nio.ByteBuffer
import kotlin.concurrent.thread

class ControlledVolumeStreamServer (core: CMMCore? = null,
                                    basePort: Int = getPropertyInt("Network.basePort"),
                                    connections: Int = getPropertyInt("Network.connections") ) {
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))
    val zContext = ZContext()
    val mmConnection = MMConnection(core_ = core)
    private val controlConnection = ControlZMQServer(zContext,basePort)
    val volumeSender = VolumeSender(microscenery.zContext,connections,basePort+1)

    val timeBetweenUpdates = 1000

    var imagingRunning = false
    var imagingThread : Thread? = null

    var status = ServerSignal.Status(Vector3i(0),ServerState.Paused, volumeSender.usedPorts())

    init {
        fun stopImaging(){
            if (status.state == ServerState.Imaging){
                logger.info("Stopping Imaging")
                imagingRunning = false
                imagingThread?.join()
                imagingThread = null
                status = status.copy( state = ServerState.Paused)
                logger.info("imaging stopped")
            }
        }

        controlConnection.addListener { signal ->
            when(signal){
                is ClientSignal.ClientSignOn -> {
                    controlConnection.sendSignal(status)
                }
                is ClientSignal.StartImaging -> {
                    if (status.state == ServerState.Paused) {
                        mmConnection.updateSize()
                        logger.info("Start MM Sender with  ${mmConnection.width}x${mmConnection.height}x${mmConnection.slices}xShort at port ${volumeSender.basePort+1}")
                        status = status.copy(
                            imageSize = Vector3i(
                                mmConnection.width,
                                mmConnection.height,
                                mmConnection.slices
                            ), state = ServerState.Imaging
                        )

                        if (imagingThread != null){
                            throw IllegalStateException("There is a reference to an imaging thread where none should be.")
                        }
                        imagingRunning = true
                        imagingThread = startImagingAndSendingThread()
                    }
                    controlConnection.sendSignal(status)
                }
                is ClientSignal.StopImaging -> {
                    stopImaging()
                    controlConnection.sendSignal(status)
                }
                is ClientSignal.Shutdown ->{
                    stopImaging()
                    controlConnection.sendSignal(status)
                    status = status.copy( state = ServerState.ShuttingDown)
                    controlConnection.sendSignal(status)
                    volumeSender.close().forEach { it.join() }
                    microscenery.zContext.destroy()
                }
            }
        }
    }

    private fun startImagingAndSendingThread(): Thread {
        val volumeSize = mmConnection.width * mmConnection.height * mmConnection.slices * Short.SIZE_BYTES
        val volumeBuffers = RingBuffer<ByteBuffer>(2, default = {
            MemoryUtil.memAlloc((volumeSize))
        })
        var time = 0L
        return thread {
            Thread.sleep(200)
            print("start capturing")
            while (imagingRunning) {
                //wait at least timeBetweenUpdates
                (System.currentTimeMillis() - time).let {
                    if (it in 1..timeBetweenUpdates)
                        Thread.sleep(timeBetweenUpdates - it)
                }
                time = System.currentTimeMillis()

                val buf = volumeBuffers.get()
                buf.clear()
                mmConnection.captureStack(buf.asShortBuffer())
                buf.rewind()
                volumeSender.sendVolume(buf)
            }
            println("stopped capturing")
        }
    }

    @Suppress("unused")
    fun start(){
        logger.info("Got Start Command")
        if (status.state == ServerState.Paused)
            controlConnection.sendInternalSignals(listOf(ClientSignal.StartImaging()))
    }

    @Suppress("unused")
    fun pause(){
        logger.info("Got Pause Command")
        if (status.state == ServerState.Imaging)
            controlConnection.sendInternalSignals(listOf(ClientSignal.StopImaging()))
    }

    @Suppress("unused")
    fun shutdown(){
        logger.info("Got Stop Command")
        controlConnection.sendInternalSignals(listOf(ClientSignal.Shutdown()))
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            ControlledVolumeStreamServer()
        }
    }
}