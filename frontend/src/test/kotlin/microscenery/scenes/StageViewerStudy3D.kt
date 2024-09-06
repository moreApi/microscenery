package microscenery.scenes

import graphics.scenery.Box
import graphics.scenery.Sphere
import graphics.scenery.attribute.material.Material
import graphics.scenery.volumes.TransferFunction
import microscenery.*
import microscenery.UI.StageSpaceUI
import microscenery.VRUI.VRUIManager
import microscenery.scenes.microscope.DemoBehavior
import microscenery.simulation.BoxSimulatable
import microscenery.simulation.Simulatable
import microscenery.simulation.SimulationMicroscopeHardware
import microscenery.simulation.SphereSimulatable
import microscenery.stageSpace.FrameGizmo
import microscenery.stageSpace.MicroscopeLayout
import microscenery.stageSpace.StageSpaceManager
import org.joml.Matrix4f
import org.joml.Vector2i
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.sqrt


class StageViewerStudy3D : DefaultScene(withSwingUI = true, width = 500, height = 500,VR=!true) {
    lateinit var stageSpaceManager: StageSpaceManager
    val msHub = MicrosceneryHub(hub)

    override fun init() {
        super.init()
        logger.info("Starting demo hw scene")
        cam.spatial().position = Vector3f(0f, 0f, 2f)


        MicroscenerySettings.set("Stage.precisionXY", 1f)
        MicroscenerySettings.set("Stage.precisionZ", 1f)
        MicroscenerySettings.set(Settings.UI.ShowSelectionIndicator, false)


        val hw = SimulationMicroscopeHardware(msHub, imageSize = Vector2i(250), maxIntensity = 5000)
        stageSpaceManager = StageSpaceManager(
            hw,
            scene,
            msHub,
            layout = MicroscopeLayout.Default(MicroscopeLayout.Axis.Z)
        )


        val tfManager = stageSpaceManager.sliceManager.transferFunctionManager

        tfManager.transferFunction = TransferFunction.ramp(distance = 1f)
        tfManager.minDisplayRange = 0f
        tfManager.maxDisplayRange = 5001f

        lightBulb()

        stageSpaceManager.focusTarget?.mode = FrameGizmo.Mode.STEERING


        thread {
            @Suppress("UNUSED_VARIABLE") val db = DemoBehavior(
                hw.hardwareDimensions().stageMax,
                stageSpaceManager
            )
        }
        thread {
            while (true) {
                Thread.sleep(200)
                scene to stageSpaceManager
            }
        }
    }

    private fun lightBulb(){

        val box = Box(Vector3f(5f,10f,5f))
        box.material().cullingMode = Material.CullingMode.FrontAndBack
        BoxSimulatable.addTo(box).also {
            it.range = 5f
            it.maxIntensity = 4000
        }
        stageSpaceManager.stageRoot.addChild(box)

        val sphere = Sphere(10f)
        sphere.material().cullingMode = Material.CullingMode.FrontAndBack
        sphere.spatial().position.y = 15f
        SphereSimulatable.addTo(sphere)
        stageSpaceManager.stageRoot.addChild(sphere)
    }

    override fun inputSetup() {
        super.inputSetup()
        val ssUI = StageSpaceUI(stageSpaceManager)
        ssUI.stageUI(this, inputHandler, msHub)

        if (VR){

            VRUIManager.initBehavior(
                scene, hmd, inputHandler,
                stageSpaceUI = ssUI, msHub = MicrosceneryHub(hub)
            )
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            StageViewerStudy3D().main()
        }
    }
}


