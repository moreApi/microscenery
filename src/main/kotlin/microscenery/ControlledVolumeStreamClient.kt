package microscenery

import MicroscenerySettings
import graphics.scenery.Hub
import graphics.scenery.Scene
import graphics.scenery.utils.LazyLogger
import graphics.scenery.volumes.Volume
import microscenery.network.*
import net.imglib2.type.numeric.integer.UnsignedShortType
import org.joml.Vector3f
import org.zeromq.ZContext

/**
 * Adds and manages the streamed volume.
 *
 * call [init] in [SceneryBase.init]
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
        val dummyVolume = Volume.fromBuffer(emptyList(), 5, 5, 5, UnsignedShortType(), hub)
        dummyVolume.spatial().position = Vector3f(999f)
        dummyVolume.name = "dummy volume"
        scene.addChild(dummyVolume)

        controlConnection.addListener { signal ->
            when (signal) {
                is ServerSignal.Status -> {
                    latestServerStatus = signal
                    when (signal.state) {
                        ServerState.Imaging -> {
                            if (signal.dataPorts.isEmpty()) {
                                logger.warn("Got imaging status but empty port list.")
                                return@addListener
                            }

                            if (mmVol?.running == true) {
                                logger.info("Got imaging status but found active streaming vol. Changing nothing")
                                return@addListener
                            }

                            // close old connections of there are any, like after a pause
                            connection?.close()?.forEach { it.join() }

                            val oldVolume = mmVol
                            mmVol?.running = false

                            // build new stuff
                            val width = signal.imageSize.x
                            val height = signal.imageSize.y
                            val slices = signal.imageSize.z

                            connection = VolumeReceiver(reuseBuffers = false,
                                zContext = zContext,
                                width * height * slices * Short.SIZE_BYTES,
                                connections = signal.dataPorts.map { host to it })
                            var time = 0L
                            val timeBetweenUpdates = 1000

                            mmVol = StreamedVolume(hub, width, height, slices) {
                                //wait at least timeBetweenUpdates
                                (System.currentTimeMillis() - time).let { delta ->
                                    if (delta in 1..timeBetweenUpdates) Thread.sleep(timeBetweenUpdates - delta)
                                }
                                time = System.currentTimeMillis()
                                connection?.getVolume(2000, it)
                            }
                            scene.addChild(mmVol!!.volume)

                            // there always has to be at least one volume in the scene otherwise the volume manager goes nuts :/
                            // by now we have at least two and we can clean the old volumes now
                            oldVolume?.let {
                                it.running = false
                                scene.removeChild(it.volume)
                            }
                            if (dummyVolume.parent != null)
                                scene.removeChild(dummyVolume)
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
            }
        }
        controlConnection.sendSignal(ClientSignal.ClientSignOn)
    }
}