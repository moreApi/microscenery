package microscenery.scenes.stageStudy

import graphics.scenery.SceneryElement
import graphics.scenery.controls.InputHandler
import graphics.scenery.controls.behaviours.ArcballCameraControl
import graphics.scenery.volumes.TransferFunctionEditor
import microscenery.*
import microscenery.UI.FrameMouseDrag
import microscenery.UI.StageSpaceUI
import microscenery.UI.StageUICommand
import microscenery.scenes.stageStudy.Orchestration.TrialCoordinator
import microscenery.simulation.ProceduralBlob
import microscenery.simulation.StageSimulation
import microscenery.simulation.StageSimulation.Companion.toggleMaterialRendering
import microscenery.simulation.TubeScenario
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector3f
import org.scijava.ui.behaviour.ClickBehaviour
import javax.swing.JFrame
import javax.swing.JOptionPane
import kotlin.concurrent.thread


class StageViewerStudy3D(
    val scenario: StageSimulation.Scenario,
    val trialCoordinator: TrialCoordinator? = null
) : DefaultScene(withSwingUI = !true, width = 1200, height = 1200, VR = false) {
    val msHub = MicrosceneryHub(hub)
    lateinit var stageSpaceManager: StageSpaceManager

    lateinit var stageSimulation: StageSimulation
    lateinit var studyLogger: StudySpatialLogger
    var targetJudge: TargetJudge? = null

    init {
        MicroscenerySettings.set("Stage.precisionXY", 1f)
        MicroscenerySettings.set("Stage.precisionZ", 3f)
        MicroscenerySettings.set(Settings.UI.ShowSelectionIndicator, false)
        MicroscenerySettings.set(Settings.VRUI.LockRotationDefault, true)
        MicroscenerySettings.set(Settings.StageSpace.ShowFocusPositionLabel, false)
    }

    override fun init() {
        super.init()
        logger.info("Starting demo hw scene")
        cam.spatial().position = Vector3f(0f, -0f, 2f)

        stageSimulation = StageSimulation()
        stageSpaceManager = stageSimulation.setupStage(msHub, scene)

        studyLogger = StudySpatialLogger(cam, msHub, null)


        val targetPositions = scenario.generate(stageSpaceManager, stageSimulation.stageSpaceSize)
        val targetBlobs = targetPositions.map {
            val blob = ProceduralBlob(size = 75)
            blob.spatial().position = it
            blob.material().diffuse = Vector3f(0f, .9f, 0f)
            stageSpaceManager.stageRoot.addChild(blob)
            blob
        }

        targetJudge = TargetJudge(targetBlobs, studyLogger, trialCoordinator)

        (scenario as? TubeScenario)?.autoExplore(stageSpaceManager, stageSimulation.imageSize)

        thread {
            waitForSceneInitialisation()

            // The dialog box should show up in the task bar. That's why we create a frame for it.
            val frame = JFrame("Ready Check")
            frame.isUndecorated = true
            frame.isVisible = true
            frame.setAlwaysOnTop(true)
            frame.setLocationRelativeTo(null)
            JOptionPane.showMessageDialog(
                frame,
                "Press ok when ready"
            )

            frame.dispose()

            initFrameAndRoIControl()
            trialCoordinator?.startCase(studyLogger, TrialCoordinator.FinishMessageDisplayer(cam))

            logger.warn("Starting!")
        }

//        thread {
//            while (true) {
//                Thread.sleep(200)
//                scene to stageSpaceManager
//            }
//        }
    }

    override fun inputSetup() {
        super.inputSetup()
        val ssUI = StageSpaceUI(stageSpaceManager)
        //ssUI.stageUI(this, inputHandler, msHub)
        val commands = listOf(ssUI.comStackAcq, ssUI.comStop, ssUI.comSteering, ssUI.comGoLive,
            StageUICommand("toggle material", "M", object : ClickBehaviour {
                override fun click(p0: Int, p1: Int) {
                    scene.toggleMaterialRendering()
                }
            }), StageUICommand("mark RoI", "button3", object : ClickBehaviour {
                override fun click(p0: Int, p1: Int) {
                    targetJudge?.hit(stageSpaceManager.focusManager.focusTarget.spatial().position)
                }
            }), StageUICommand("transfer function", "T", object : ClickBehaviour {
                override fun click(p0: Int, p1: Int) {
                    TransferFunctionEditor.showTFFrame(stageSpaceManager.sliceManager.transferFunctionManager)
                }
            })
        )

        this.extraPanel?.let { ssUI.stageSwingUI(it, msHub, commands) }
        this.mainFrame?.pack()

        inputHandler?.let { inputHandler ->
            ssUI.stageKeyUI(inputHandler, cam, commands)
        }

        // disable fps camera control
        inputHandler?.addBehaviour("mouse_control", ClickBehaviour { _, _ -> /*dummy*/ })
    }

    private fun initFrameAndRoIControl() {
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
            FrameMouseDrag(stageSpaceManager.focusManager.focusTarget, cam, { 25f }),
            "button1",
            "scroll",
            name = "Frame Mouse Drag"
        )
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            //StageViewerStudy3D(AxionScenario()).main()
            StageViewerStudy3D(TubeScenario()).main()
        }
    }
}



