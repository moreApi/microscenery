package microscenery.scenes

import graphics.scenery.Box
import graphics.scenery.attribute.material.Material
import graphics.scenery.primitives.Atmosphere
import microscenery.*
import microscenery.UI.StageSpaceUI
import microscenery.VRUI.VRUIManager
import microscenery.stageSpace.MicroscopeLayout
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector3f
import kotlin.concurrent.thread


class RappNeuroStack : DefaultScene("RappNeuroStack", VR = !false, width = 840, height = 840) {

    val atmosphere = !true

    init {
        val setts = listOf(
            Settings.StageSpace.viewMode to true,
            Settings.StageSpace.HideFocusFrame to true,
            Settings.StageSpace.HideFocusTargetFrame to true,
            Settings.StageSpace.HideStageSpaceLabel to true,
            Settings.VRToolbox.CroppingEnabled to true,
            Settings.VRToolbox.MeasuringEnabled to true,
            Settings.VRToolbox.PathAblationEnabled to true,
            Settings.VRToolbox.PointAblationEnabled to true,
        )
        setts.forEach { MicroscenerySettings.set(it.first, it.second) }
        MicroscenerySettings.set(Settings.StageSpace.ShowHullbox, !atmosphere)
        MicroscenerySettings.set(Settings.Ablation.SizeUM, 10f)
    }

    val two = """H:\volumes\Neuronenstacks\img_00115_unsigned_short.tif"""
    val three = """H:\volumes\Neuronenstacks\img_00118_unsigned_short.tif"""
    val four = """H:\volumes\Neuronenstacks\img_00123_unsigned_short.tif"""
    val imgFile = two

    lateinit var stageSpaceManager: StageSpaceManager
    val msHub = MicrosceneryHub(hub)
    val microscope = FileMicroscopeHardware(imgFile, zPerXY = 2f)


    override fun init() {
        super.init()

        if (atmosphere) {
            val atmos = Atmosphere(emissionStrength = 0.3f, initSunDirection = Vector3f(-0.75f, 0.1f, -1.0f))
            scene.addChild(atmos)

            val floor = Box(Vector3f(6.0f, 0.30f, 5.0f), insideNormals = true)
            floor.material {
                ambient = Vector3f(0.6f, 0.6f, 0.6f)
                diffuse = Vector3f(0.4f, 0.4f, 0.4f)
                specular = Vector3f(0.0f, 0.0f, 0.0f)
                cullingMode = Material.CullingMode.Front
            }
            floor.spatial { position = Vector3f(0f, -1f, 1.25f) }
            scene.addChild(floor)
        }
        cam.spatial().position = Vector3f(0f, if (VR) -1f else 0f, 1f)

        stageSpaceManager = StageSpaceManager(
            microscope,
            scene,
            msHub,
            layout = MicroscopeLayout.Default(MicroscopeLayout.Axis.Z)
//            layout = MicroscopeLayout.Scape(MicroscopeLayout.Axis.Y, 33f)
        )

        stageSpaceManager.sliceManager.transferFunctionManager.maxDisplayRange = 60000f
        stageSpaceManager.sliceManager.transferFunctionManager.minDisplayRange = 2100f


        //microscope.loadImg(it)
        stageSpaceManager.stack(Vector3f(), Vector3f())

        thread {
            while (true) {
                Thread.sleep(200)
                stageSpaceManager
            }
        }

    }

    override fun inputSetup() {
        super.inputSetup()

        val ssUI = StageSpaceUI(stageSpaceManager)

        inputHandler?.let {
            ssUI.stageKeyUI(it, cam)
        }

        if (VR) {
            VRUIManager.initBehavior(
                scene, hmd, inputHandler,
                stageSpaceUI = ssUI, msHub = msHub
            )
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            RappNeuroStack().main()
        }
    }
}




