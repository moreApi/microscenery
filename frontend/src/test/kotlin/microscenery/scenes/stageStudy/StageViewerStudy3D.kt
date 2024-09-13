package microscenery.scenes.stageStudy

import graphics.scenery.SceneryElement
import graphics.scenery.controls.InputHandler
import graphics.scenery.controls.behaviours.ArcballCameraControl
import graphics.scenery.volumes.TransferFunction
import microscenery.*
import microscenery.UI.FrameMouseDrag
import microscenery.UI.StageSpaceUI
import microscenery.VRUI.VRUIManager
import microscenery.scenes.microscope.DemoBehavior
import microscenery.simulation.SimulationMicroscopeHardware
import microscenery.stageSpace.MicroscopeLayout
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector2i
import org.joml.Vector3f
import kotlin.concurrent.thread


class StageViewerStudy3D : DefaultScene(withSwingUI = true, width = 500, height = 500,VR=!true) {
    lateinit var stageSpaceManager: StageSpaceManager
    val msHub = MicrosceneryHub(hub)

    override fun init() {
        super.init()
        logger.info("Starting demo hw scene")
        cam.spatial().position = Vector3f(0f, -1f, 2f)


        MicroscenerySettings.set("Stage.precisionXY", 1f)
        MicroscenerySettings.set("Stage.precisionZ", 3f)
        MicroscenerySettings.set(Settings.UI.ShowSelectionIndicator, false)

        stageSpaceManager = StageSimulation.setupStage(msHub, scene)
        StageSimulation.scaffold(stageSpaceManager.stageRoot)

        thread {
            while (true) {
                Thread.sleep(200)
                scene to stageSpaceManager
            }
        }
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
        } else {
            val windowWidth = renderer?.window?.width ?: 512
            val windowHeight = renderer?.window?.height ?: 512

            val target =  Vector3f(0.0f)
            val inputHandler = (hub.get(SceneryElement.Input) as InputHandler)
            val targetArcball = ArcballCameraControl("mouse_control", { scene.findObserver() }, windowWidth, windowHeight, target)

            targetArcball.target = { target }

            inputHandler.addBehaviour("mouse_control", targetArcball)
            inputHandler.addBehaviour("scroll_arcball", targetArcball)
            //inputHandler.addKeyBinding("scroll_arcball", "scroll")

            val frameMouseDrag = FrameMouseDrag(stageSpaceManager.focusManager.focusTarget,{25f})
            inputHandler.addBehaviour(frameMouseDrag.name,frameMouseDrag)
            inputHandler.addKeyBinding(frameMouseDrag.name,"button2")
            inputHandler.addKeyBinding(frameMouseDrag.name,"scroll")
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            StageViewerStudy3D().main()
        }
    }

}


