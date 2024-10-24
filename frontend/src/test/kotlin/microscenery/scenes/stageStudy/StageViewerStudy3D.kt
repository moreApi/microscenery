package microscenery.scenes.stageStudy

import graphics.scenery.SceneryElement
import graphics.scenery.controls.InputHandler
import graphics.scenery.controls.behaviours.ArcballCameraControl
import graphics.scenery.volumes.TransferFunction
import microscenery.*
import microscenery.UI.DesktopUI
import microscenery.UI.FrameMouseDrag
import microscenery.UI.StageSpaceUI
import microscenery.UI.StageUICommand
import microscenery.VRUI.VRUIManager
import microscenery.simulation.ProceduralBlob
import microscenery.simulation.Simulatable
import microscenery.simulation.StageSimulation
import microscenery.simulation.StageSimulation.Companion.toggleMaterialRendering
import microscenery.stageSpace.FocusManager
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector3f
import org.scijava.ui.behaviour.ClickBehaviour
import kotlin.concurrent.thread
import kotlin.random.Random


class StageViewerStudy3D : DefaultScene(withSwingUI = true, width = 1200, height = 1200, VR = !true) {
    lateinit var stageSpaceManager: StageSpaceManager
    lateinit var stageSimulation: StageSimulation
    val msHub = MicrosceneryHub(hub)


    override fun init() {
        super.init()
        logger.info("Starting demo hw scene")
        cam.spatial().position = Vector3f(0f, -0f, 2f)


        MicroscenerySettings.set("Stage.precisionXY", 1f)
        MicroscenerySettings.set("Stage.precisionZ", 3f)
        MicroscenerySettings.set(Settings.UI.ShowSelectionIndicator, false)

        val seed = Random.nextInt()
        val random = Random(seed)
        println("Seed: $seed")

        stageSimulation = StageSimulation(imageSize = 150, random = random)
        stageSpaceManager = stageSimulation.setupStage(msHub, scene)
//        val targetPositions = stageSimulation.scaffold(stageSpaceManager.stageRoot)
//        val targetPositions = stageSimulation.tubeScenario(stageSpaceManager.stageRoot)
        val targetPositions = stageSimulation.axionScenario(stageSpaceManager.stageRoot)
//        val targetPositions: List<Vector3f> = listOf()
        //targetPositions.random().let {
        targetPositions.forEach {
            val blob = ProceduralBlob(size = 75)
            blob.spatial().position = it
            stageSpaceManager.stageRoot.addChild(blob)
        }

        scene.discover { it.getAttributeOrNull(Simulatable::class.java) != null }


        stageSpaceManager.sliceManager.transferFunctionManager.apply {
            this.transferFunction = TransferFunction.ramp(0f, 1f, 1f)
            this.transferFunction.controlPoints().first().factor = 0.05f
        }


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
        //ssUI.stageUI(this, inputHandler, msHub)
        val commands = listOf(ssUI.comStackAcq, ssUI.comStop, ssUI.comSteering, ssUI.comGoLive,
            StageUICommand("seach Cube", "button3", object : ClickBehaviour {
                override fun click(p0: Int, p1: Int) {
                    ssUI.comSearchCube.command?.click(0, 0)
                    if (ssUI.searchCubeStart == null) {
                        thread {
                            Thread.sleep(3000)
                            stageSpaceManager.goLive()
                            stageSpaceManager.focusManager.mode = FocusManager.Mode.STEERING
                        }
                    }
                }
            }), StageUICommand("toggle material", null, object : ClickBehaviour {
                override fun click(p0: Int, p1: Int) {
                    scene.toggleMaterialRendering()
                }
            })
        )

        this.extraPanel?.let { ssUI.stageSwingUI(it, msHub, commands) }
        this.mainFrame?.pack()
        DesktopUI.initMouseSelection(inputHandler, msHub)

        inputHandler?.let { inputHandler ->
            ssUI.stageKeyUI(inputHandler, cam, commands)
        }


        if (VR) {
            VRUIManager.initBehavior(
                scene, hmd, inputHandler,
                stageSpaceUI = ssUI, msHub = MicrosceneryHub(hub)
            )
        } else {
            // disable fps camera control
            inputHandler?.addBehaviour("mouse_control", ClickBehaviour { _, _ -> /*dummy*/ })

            val windowWidth = renderer?.window?.width ?: 512
            val windowHeight = renderer?.window?.height ?: 512

            val target = Vector3f(0.0f)
            val inputHandler = (hub.get(SceneryElement.Input) as InputHandler)
            val targetArcball =
                ArcballCameraControl("mouse_control", { scene.findObserver() }, windowWidth, windowHeight, {
                    stageSpaceManager.focusManager.focusTarget.spatial().worldPosition()
                })

            targetArcball.target = { target }

            inputHandler.addBehaviour("arcCam", targetArcball)
            inputHandler.addKeyBinding("arcCam", "button2")

            inputHandler.addBehaviorBinding(
                FrameMouseDrag(stageSpaceManager.focusManager.focusTarget, { 25f }),
                "button1",
                "scroll",
                name = "Frame Mouse Drag"
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


