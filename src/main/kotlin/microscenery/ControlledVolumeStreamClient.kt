package microscenery

import MicroscenerySettings
import graphics.scenery.Hub
import graphics.scenery.Scene
import graphics.scenery.primitives.TextBoard
import graphics.scenery.utils.LazyLogger
import microscenery.network.*
import org.joml.Vector4f
import org.zeromq.ZContext

/**
 * Adds and manages the streamed volume.
 *
 * call [init] in SceneryBase.init
 */
class ControlledVolumeStreamClient(
    val scene: Scene,
    val hub: Hub,
    basePort: Int = MicroscenerySettings.get("Network.basePort"),
    val host: String = MicroscenerySettings.get("Network.host"),
    val zContext: ZContext = ZContext()
) {
    private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))
    private val controlConnection = ControlZMQClient(zContext, basePort, host)

    var mmVol: StreamedVolume? = null
    var connection: VolumeReceiver? = null

    var latestServerStatus: ServerSignal.Status? = null
    var lastAcquisitionSignal = 0L

    @Suppress("unused")
    fun start() {
        logger.info("Got Start Command")
        if (latestServerStatus?.state == ServerState.Paused) controlConnection.sendSignal(ClientSignal.StartImaging)
    }

    @Suppress("unused")
    fun snap() {
        logger.info("Got Snap Command")
        if (latestServerStatus?.state == ServerState.Paused) controlConnection.sendSignal(ClientSignal.SnapStack)
    }

    @Suppress("unused")
    fun pause() {
        logger.info("Got Pause Command")
        if (latestServerStatus?.state == ServerState.Imaging) controlConnection.sendSignal(ClientSignal.StopImaging)
    }

    @Suppress("unused")
    fun shutdown() {
        logger.info("Got Stop Command")
        controlConnection.sendSignal(ClientSignal.Shutdown)
    }

    /**
     * Adds a dummy Volume to the scene to avoid a bug later and adds and manages the streamed volume.
     */
    fun init() {
        // Required so the volumeManager is initialized later in the scene -.-
        /*val dummyVolume = Volume.fromBuffer(emptyList(), 5, 5, 5, UnsignedShortType(), hub)
        dummyVolume.spatial().position = Vector3f(999f)
        dummyVolume.name = "dummy volume"
        dummyVolume.addTimepoint("bums", MemoryUtil.memAlloc(5 * 5 * 5 * Short.SIZE_BYTES))
        scene.addChild(dummyVolume)*/

        controlConnection.addListener { signal ->
            when (signal) {
                is ServerSignal.Status -> {
                    latestServerStatus = signal
                    when (signal.state) {
                        ServerState.Imaging -> {
                            if (!refresh(signal)) return@addListener
                            mmVol?.paused = false
                        }
                        ServerState.Paused -> {
                            mmVol?.paused = true
                        }
                        ServerState.ShuttingDown -> {
                            mmVol?.running = false
                            connection?.close()?.forEach { it.join() }
                            logger.warn("Server shutdown")
                        }
                        ServerState.Snapping -> {}
                    }
                }
                is ServerSignal.StackAcquired -> {
                    lastAcquisitionSignal = System.currentTimeMillis()
                    latestServerStatus?.let {
                        if (it.state == ServerState.Imaging) return@addListener // the live imaging will take care of this stack
                        if (!refresh(it)) return@addListener
                        mmVol?.once = true
                    } ?: let {
                        logger.warn("Got stack signal but do not know the server status.")
                    }
                }
            }
        }
        controlConnection.sendSignal(ClientSignal.ClientSignOn)
    }

    private fun refresh(signal: ServerSignal.Status): Boolean {
        if (signal.dataPorts.isEmpty()) {
            logger.warn("Got imaging status but empty port list.")
            return false
        }

        if (mmVol?.paused == false && mmVol?.running == true) {
            logger.info("Found active streaming vol. Changing nothing")
            return false
        }

        // build new stuff
        val width = signal.imageSize.x
        val height = signal.imageSize.y
        val slices = signal.imageSize.z

        if (width == mmVol?.width && height == mmVol?.height && slices == mmVol?.depth)
            return true // no need the create new volume

        // close old connections of there are any
        connection?.close()?.forEach { it.join() }

        val oldVolume = mmVol
        mmVol?.running = false

        connection = VolumeReceiver(reuseBuffers = false,
            zContext = zContext,
            width * height * slices * Short.SIZE_BYTES,
            connections = signal.dataPorts.map { host to it })

        mmVol = StreamedVolume(hub, width, height, slices, getData = {
            connection?.getVolume(2000, it)
        }, paused = true)
        oldVolume?.let {
            it.running = false
            scene.removeChild(it.volume)
        }
        scene.addChild(mmVol!!.volume)

        return true
    }


    fun lastAcquisitionTextBoard(): TextBoard {
        val lastUpdateBoard = TextBoard()
        lastUpdateBoard.text = "Last Acquisition Signal: never"
        lastUpdateBoard.transparent = 0
        lastUpdateBoard.fontColor = Vector4f(0.0f, 0.0f, 0.0f, 1.0f)
        lastUpdateBoard.backgroundColor = Vector4f(100f, 100f, 100f, 1.0f)
        lastUpdateBoard.update += {
            if (this.lastAcquisitionSignal != 0L) {
                val dif = System.currentTimeMillis() - this.lastAcquisitionSignal
                lastUpdateBoard.text = "Last Acquisition Signal: " + (dif / 100).toFloat() / 10 + "sec"
            }
        }
        return lastUpdateBoard
    }
}