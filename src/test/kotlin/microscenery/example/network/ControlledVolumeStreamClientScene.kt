package microscenery.example.network

import getPropertyInt
import getPropertyString
import graphics.scenery.*
import graphics.scenery.backends.Renderer
import graphics.scenery.volumes.Volume
import lightSleepOn
import microscenery.StreamedVolume
import microscenery.network.*
import org.joml.Vector3f
import org.zeromq.ZContext
import kotlin.concurrent.thread

class ControlledVolumeStreamClientScene(
    val basePort: Int = getPropertyInt("Network.basePort"),
    val host: String = getPropertyString("Network.host"))
    : SceneryBase(
    ControlledVolumeStreamClientScene::class.java.simpleName,
    windowWidth = 1920,
    windowHeight = 1200,
    wantREPL = true
) {
    private val zContext = ZContext()
    private val controlConnection = ControlZMQClient(zContext, basePort, host)

    var mmVol: StreamedVolume? = null
    var connection: VolumeReceiver? = null

    var latestServerStatus : ServerSignal.Status? = null

    fun start(){
        logger.info("Got Start Command")
        if (latestServerStatus?.state == ServerState.Paused)
            controlConnection.sendSignal(ClientSignal.StartImaging())
    }

    fun pause(){
        logger.info("Got Pause Command")
        if (latestServerStatus?.state == ServerState.Imaging)
            controlConnection.sendSignal(ClientSignal.StopImaging())
    }

    fun shutdown(){
        logger.info("Got Stop Command")
        controlConnection.sendSignal(ClientSignal.Shutdown())
    }

    override fun init() {
        baseInit()
        initDummyNode(scene)

        controlConnection.addListener { signal ->
            when(signal){
                is ServerSignal.Status -> {
                    latestServerStatus = signal
                    when(signal.state){
                        ServerState.Imaging -> {
                            if (signal.dataPorts.isEmpty()){
                                logger.warn("Got imaging status but empty port list.")
                                return@addListener
                            }

                            // clean old stuff if there
                            mmVol?.let {
                                if (it.running) {
                                    logger.info("Got imaging status but found active streaming vol. Changing nothing")
                                    return@addListener
                                } else {
                                    scene.removeChild(it.volume)
                                }

                            }
                            connection?.close()?.forEach { it.join() }

                            // build new stuff
                            val width = signal.imageSize.x
                            val height = signal.imageSize.y
                            val slices = signal.imageSize.z

                            connection = VolumeReceiver(
                                reuseBuffers = false,
                                zContext = zContext,
                                width * height * slices * Short.SIZE_BYTES,
                                connections = signal.dataPorts.map { host to it}
                            )
                            var time = 0L
                            val timeBetweenUpdates = 1000
                            mmVol = StreamedVolume(hub, width, height, slices) {
                                //wait at least timeBetweenUpdates
                                (System.currentTimeMillis() - time).let { delta ->
                                    if (delta in 1..timeBetweenUpdates)
                                        Thread.sleep(timeBetweenUpdates - delta)
                                }
                                time = System.currentTimeMillis()
                                connection?.getVolume(2000, it)
                            }
                            scene.addChild(mmVol!!.volume)
                        }
                        ServerState.Paused -> {
                            mmVol?.running = false
                            connection?.close()
                        }
                        ServerState.ShuttingDown -> TODO()
                    }
                }
            }
        }
        controlConnection.sendSignal(ClientSignal.ClientSignOn())
    }

    /**
     * Required so the volumeManager is initialized later in the scene -.-
     */
    private fun initDummyNode(scene: Scene) {
        val head = Volume.VolumeFileSource(
            Volume.VolumeFileSource.VolumePath.Given("C:\\Users\\JanCasus\\volumes\\t1-head.tif"),
            Volume.VolumeFileSource.VolumeType.DEFAULT
        )
        val dummyVolume = Volume.forNetwork(head, hub)
        dummyVolume.spatial().position = Vector3f(999f)
        scene.addChild(dummyVolume)
    }

    private fun baseInit() {
        renderer = hub.add(
            SceneryElement.Renderer,
            Renderer.createRenderer(hub, applicationName, scene, 512, 512)
        )

        val light = PointLight(radius = 15.0f)
        light.spatial().position = Vector3f(0.0f, 0.0f, 2.0f)
        light.intensity = 5.0f
        light.emissionColor = Vector3f(1.0f, 1.0f, 1.0f)
        scene.addChild(light)

        val cam: Camera = DetachedHeadCamera()
        with(cam) {
            spatial {
                position = Vector3f(0.0f, 0.0f, 15.0f)
            }
            perspectiveCamera(50.0f, width, height)

            scene.addChild(this)
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val b = ControlledVolumeStreamClientScene()
            thread {
                lightSleepOn(15000) { b.latestServerStatus }
                println("status here")
                b.start()
            }
            b.main()
        }
    }
}