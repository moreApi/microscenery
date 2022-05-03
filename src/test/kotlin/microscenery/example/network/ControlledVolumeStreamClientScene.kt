package microscenery.example.network

import getPropertyInt
import getPropertyString
import graphics.scenery.*
import graphics.scenery.backends.Renderer
import microscenery.StreamedVolume
import microscenery.network.*
import org.joml.Vector3f
import org.zeromq.ZContext

class ControlledVolumeStreamClientScene(
    val basePort: Int = getPropertyInt("Network.basePort"),
    val host: String = getPropertyString("Network.host"))
    : SceneryBase(
    ControlledVolumeStreamClientScene::class.java.simpleName,
    windowWidth = 1920,
    windowHeight = 1200,
    wantREPL = false
) {
    private val zContext = ZContext()
    private val controlConnection = ControlZMQClient(zContext, basePort, host)

    lateinit var mmVol: StreamedVolume


    override fun init() {
        baseInit()

        controlConnection.addListener { signal ->
            when(signal){
                is ServerSignal.Status -> {
                    when(signal.state){
                        ServerState.Imaging -> {
                            if (signal.dataPorts.isEmpty()){
                                logger.warn("Got imaging status but empty port list.")
                                return@addListener
                            }

                            val width = signal.imageSize.x
                            val height = signal.imageSize.y
                            val slices = signal.imageSize.z

                            val connection = VolumeReceiver(
                                reuseBuffers = false,
                                zContext = zContext,
                                width * height * slices * Short.SIZE_BYTES,
                                connections = signal.dataPorts.map { host to it}
                            )
                            var time = 0L
                            val timeBetweenUpdates = 1000
                            mmVol = StreamedVolume(hub, width, height, slices) {
                                //wait at least timeBetweenUpdates
                                (System.currentTimeMillis() - time).let {
                                    if (it in 1..timeBetweenUpdates)
                                        Thread.sleep(timeBetweenUpdates - it)
                                }
                                time = System.currentTimeMillis()
                                connection.getVolume(2000, it)
                            }
                            scene.addChild(mmVol.volume)
                        }
                        ServerState.Paused -> {}
                        ServerState.ShuttingDown -> TODO()
                    }
                }
            }
        }

        controlConnection.sendSignal(ClientSignal.StartImaging())


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
            ControlledVolumeStreamClientScene().main()
        }
    }
}