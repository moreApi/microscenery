package microscenery

import MicroscenerySettings
import graphics.scenery.Hub
import graphics.scenery.Scene
import graphics.scenery.utils.LazyLogger
import microscenery.network.*
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

    @Suppress("unused")
    fun start() {
        logger.info("Got Start Command")
        if (latestServerStatus?.state == ServerState.Paused) controlConnection.sendSignal(ClientSignal.StartImaging)
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
                            mmVol?.running = false
                            connection?.close()
                        }
                        ServerState.ShuttingDown -> {
                            mmVol?.running = false
                            connection?.close()?.forEach { it.join() }
                            logger.warn("Server shutdown")
                        }
                    }
                }
                ServerSignal.StackAcquired -> {
                    latestServerStatus?.let {
                        if (!refresh(it)) return@addListener
                        mmVol?.paused = false
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

        if (mmVol?.running == true) {
            logger.info("Got imaging status but found active streaming vol. Changing nothing")
            return false
        }

        // build new stuff
        val width = signal.imageSize.x
        val height = signal.imageSize.y
        val slices = signal.imageSize.z

        if(width == mmVol?.width && height == mmVol?.height && slices == mmVol?.depth)
            connection // no need the create new volume

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
}