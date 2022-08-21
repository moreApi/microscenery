package microscenery

import MicroscenerySettings
import graphics.scenery.utils.LazyLogger
import graphics.scenery.utils.RingBuffer
import kotlinx.event.event
import microscenery.network.*
import mmcorej.CMMCore
import org.joml.Vector3i
import org.lwjgl.system.MemoryUtil
import org.zeromq.ZContext
import java.nio.ByteBuffer
import kotlin.concurrent.thread
import kotlin.properties.Delegates


class ControlledVolumeStreamServer @JvmOverloads constructor(
    core: CMMCore? = null,
    val basePort: Int = MicroscenerySettings.get("Network.basePort"),
    val connections: Int = MicroscenerySettings.get("Network.connections"),
    val timeBetweenUpdates: Int = MicroscenerySettings.get("MMConnection.TimeBetweenStackAcquisition",1000)
) {
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))
    val zContext = ZContext()
    val mmConnection = MMConnection(core_ = core)
    private val controlConnection = ControlZMQServer(zContext, basePort)
    val volumeSender = VolumeSender(microscenery.zContext, connections, basePort + 1)


    var imagingRunning = false
    var imagingThread: Thread? = null

    val statusChange = event<ServerSignal.Status>()
    var status by Delegates.observable(
        ServerSignal.Status(
            Vector3i(0), ServerState.Paused, volumeSender.usedPorts()
        )
    ) { _, _, newStatus: ServerSignal.Status ->
        statusChange(newStatus)
    }
    private set

    init {
        statusChange += {
            controlConnection.sendSignal(it)
        }

        fun stopImaging() {
            if (status.state == ServerState.Imaging) {
                logger.info("Stopping Imaging")
                imagingRunning = false
                imagingThread?.join()
                imagingThread = null
                status = status.copy(state = ServerState.Paused)
                logger.info("imaging stopped")
            }
        }

        controlConnection.addListener { signal ->
            when (signal) {
                is ClientSignal.ClientSignOn -> {
                    status = status.copy(connectedClients = controlConnection.connectedClients)
                }
                is ClientSignal.StartImaging -> {
                    if (status.state == ServerState.Paused) {
                        mmConnection.updateSize()
                        mmConnection.updateParameters()
                        logger.info("Start MM Sender with  ${mmConnection.width}x${mmConnection.height}x${mmConnection.steps}xShort at port ${volumeSender.basePort + 1}")
                        status = status.copy(
                            imageSize = Vector3i(
                                mmConnection.width, mmConnection.height, mmConnection.steps
                            ), state = ServerState.Imaging
                        )

                        if (imagingThread != null) {
                            throw IllegalStateException("There is a reference to an imaging thread where none should be.")
                        }
                        imagingThread = startImagingAndSendingThread()
                    }
                }
                is ClientSignal.StopImaging -> {
                    stopImaging()
                }
                is ClientSignal.Shutdown -> {
                    stopImaging()
                    status = status.copy(state = ServerState.ShuttingDown)
                    volumeSender.close().forEach { it.join() }
                    microscenery.zContext.destroy()
                }
                ClientSignal.SnapStack -> {
                    if (status.state == ServerState.Paused) {
                        mmConnection.updateSize()
                        mmConnection.updateParameters()
                        logger.info("Start MM Sender with  ${mmConnection.width}x${mmConnection.height}x${mmConnection.steps}xShort at port ${volumeSender.basePort + 1}")
                        status = status.copy(
                            imageSize = Vector3i(
                                mmConnection.width, mmConnection.height, mmConnection.steps
                            ), state = ServerState.Imaging
                        )

                        if (imagingThread != null) {
                            throw IllegalStateException("There is a reference to an imaging thread where none should be.")
                        }
                        imagingThread = startImagingAndSendingThread(true)
                    }

                }
            }
        }
    }

    private fun startImagingAndSendingThread(once: Boolean = false): Thread {
        val volumeSize = mmConnection.width * mmConnection.height * mmConnection.steps * Short.SIZE_BYTES
        val volumeBuffers = RingBuffer<ByteBuffer>(2, default = {
            MemoryUtil.memAlloc((volumeSize))
        })
        var time = 0L
        imagingRunning = true
        return thread {
            Thread.sleep(200)
            print("start capturing")

            while (imagingRunning) {
                //wait at least timeBetweenUpdates
                (System.currentTimeMillis() - time).let {
                    if (it in 1..timeBetweenUpdates) Thread.sleep(timeBetweenUpdates - it)
                }
                time = System.currentTimeMillis()

                val buf = volumeBuffers.get()
                buf.clear()
                mmConnection.captureStack(buf.asShortBuffer())
                buf.rewind()
                volumeSender.sendVolume(buf)
                controlConnection.sendSignal(ServerSignal.StackAcquired)
                if (once)
                    imagingRunning = false
            }
            println("stopped capturing")
        }
    }

    @Suppress("unused")
    fun start() {
        logger.info("Got Start Command")
        if (status.state == ServerState.Paused) controlConnection.sendInternalSignals(listOf(ClientSignal.StartImaging))
    }

    @Suppress("unused")
    fun pause() {
        logger.info("Got Pause Command")
        if (status.state == ServerState.Imaging) controlConnection.sendInternalSignals(listOf(ClientSignal.StopImaging))
    }

    @Suppress("unused")
    fun shutdown() {
        logger.info("Got Stop Command")
        controlConnection.sendInternalSignals(listOf(ClientSignal.Shutdown))
    }

    /**
     * Access settings. Java comparability function
     */
    @Suppress("unused")
    fun getSettings() = MicroscenerySettings

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            ControlledVolumeStreamServer()
        }
    }
}