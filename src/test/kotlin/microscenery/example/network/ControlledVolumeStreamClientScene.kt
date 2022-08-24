package microscenery.example.network

import graphics.scenery.*
import graphics.scenery.backends.Renderer
import graphics.scenery.utils.extensions.times
import graphics.scenery.volumes.TransferFunction
import microscenery.lightSleepOn
import microscenery.ControlledVolumeStreamClient
import microscenery.UI.DisplayRangeEditor
import org.joml.Vector3f
import kotlin.concurrent.thread


class ControlledVolumeStreamClientScene : SceneryBase(
    ControlledVolumeStreamClientScene::class.java.simpleName, windowWidth = 1920, windowHeight = 1200, wantREPL = false
) {

    val cvsc = ControlledVolumeStreamClient(scene, hub)

    override fun init() {
        baseInit()
        cvsc.init()

    }

    private fun baseInit() {
        renderer = hub.add(
            SceneryElement.Renderer, Renderer.createRenderer(hub, applicationName, scene, 512, 512)
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

        val lastUpdateBoard = cvsc.lastAcquisitionTextBoard()
        lastUpdateBoard.spatial {
            position = Vector3f(0f, 9f, -9f)
            scale = Vector3f(0.5f, 0.5f, 0.5f)
            //rotation = Quaternionf().rotationX(-PI.toFloat())
        }
        scene.addChild(lastUpdateBoard)

        thread {
            while (cvsc.mmVol == null) {
                Thread.sleep(200)
            }
            cvsc.mmVol?.let {
                it.volume.spatial().scale= Vector3f(0.225f,0.225f,3.348f) * 0.3f
                //it.volume.transferFunction = TransferFunction.ramp(0.0027f,1f,0.1f)
                it.volume.transferFunction = TransferFunction.ramp()
            //it.volume.transferFunction = TransferFunction.ramp(0.002934f,1f,0.01f)

                DisplayRangeEditor(it.volume.converterSetups.first()).isVisible = true
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val b = ControlledVolumeStreamClientScene()
            thread {
                lightSleepOn(15000) { b.cvsc.latestServerStatus }
                println("status here")
                b.cvsc.snap()
                thread {
                    while (true) {
                        Thread.sleep(200)
                        @Suppress("UNUSED_EXPRESSION")
                        b
                    }
                }
            }
            b.main()
        }
    }
}