package anon.scenes.stageStudy

import fromScenery.utils.extensions.plus
import graphics.scenery.Node
import graphics.scenery.RichNode
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackerRole
import graphics.scenery.controls.behaviours.PerButtonPressable
import graphics.scenery.controls.behaviours.Pressable
import graphics.scenery.controls.behaviours.SimplePressable
import graphics.scenery.controls.behaviours.VRTouch
import graphics.scenery.utils.extensions.times
import anon.*
import anon.UI.UIModel
import anon.VRUI.Gui3D.Button
import anon.VRUI.Gui3D.Column
import anon.VRUI.Gui3D.TextBox
import anon.VRUI.InHandForwarder
import anon.VRUI.VRHandTool
import anon.VRUI.VRUIManager
import anon.VRUI.behaviors.VR2HandSpatialManipulation
import anon.VRUI.behaviors.VRGrabTheWorldSelfMove
import anon.scenes.stageStudy.Orchestration.TrialCoordinator
import anon.simulation.AxonScenario
import anon.simulation.ProceduralBlob
import anon.simulation.StageSimulation
import anon.simulation.TubeScenario
import anon.stageSpace.FocusManager.Mode
import anon.stageSpace.StageSpaceManager
import org.joml.Vector3f
import kotlin.concurrent.thread


class StageViewerStudyVR(
    val scenario: StageSimulation.Scenario,
    val trialCoordinator: TrialCoordinator? = null
) : DefaultScene(withSwingUI = false, width = 1200, height = 1200, VR = true) {
    val msHub = MicrosceneryHub(hub)
    lateinit var stageSpaceManager: StageSpaceManager

    lateinit var stageSimulation: StageSimulation
    lateinit var targetJudge: TargetJudge
    lateinit var studyLogger: StudySpatialLogger
    lateinit var uiModel: UIModel


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
        cam.spatial().position = Vector3f(0f, -1f, 1f)

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


        thread {
            waitForSceneInitialisation()
            (scenario as? TubeScenario)?.autoExplore(stageSpaceManager, stageSimulation.imageSize)

            val target = stageSpaceManager.scaleAndRotationPivot.spatial()
            val pivot = stageSpaceManager.stageRoot.spatial().worldPosition(stageSpaceManager.stageAreaCenter)
            val scaleDelta = 0.6f
            // pivot and target are in same space
            for (i in 0..2) {
                target.position.setComponent(i, (target.position[i] + pivot[i] * (scaleDelta - 1)))
            }
            target.scale *= scaleDelta
            target.needsUpdate = true

            // greeting label and button
            var greeting: Node? = null
            greeting = Column(
                TextBox(scenario.name),
                Button("ready?") {
                    greeting?.detach()
                    logger.warn("Starting!")
                    trialCoordinator?.startCase(studyLogger, TrialCoordinator.FinishMessageDisplayer(cam, distance = 0.5f))

                    // init focus frame movement
                    val dominantHandRight = trialCoordinator?.trialConfig?.rightHanded ?: MicroscenerySettings.get(Settings.VRUI.DominantHandRight, true)
                    StudyFocusMover(stageSpaceManager, targetJudge).activate(uiModel, if (dominantHandRight) TrackerRole.RightHand else TrackerRole.LeftHand)
                }, middleAlign = true, invertedYOrder = true).apply {
                this.spatial {
                    this.position = stageSpaceManager.stageAreaCenter.copy() + Vector3f(-200f, -100f, 200f)
                    this.scale = Vector3f(200f)
                }
                stageSpaceManager.stageRoot.addChild(this)
            }
        }

    }

    override fun inputSetup() {
        super.inputSetup()
        uiModel = VRUIManager.initUIModel(msHub, hmd)


        VRGrabTheWorldSelfMove.createAndSet(
            scene,
            hmd,
            listOf(OpenVRHMD.OpenVRButton.A, OpenVRHMD.OpenVRButton.Menu),
            listOf(TrackerRole.RightHand, TrackerRole.LeftHand)
        )

        VR2HandSpatialManipulation.createAndSet(
            hmd,
            OpenVRHMD.OpenVRButton.Side,
            scene,
            stageSpaceManager = stageSpaceManager
        )


        val pressButtons = listOf(
            OpenVRHMD.OpenVRButton.Trigger
        )
        InHandForwarder.createAndWrapVRPressWithInHandManagerBehavior(
            uiModel,
            scene,
            hmd,
            TrackerRole.LeftHand,
            pressButtons
        )
        InHandForwarder.createAndWrapVRPressWithInHandManagerBehavior(
            uiModel,
            scene,
            hmd,
            TrackerRole.RightHand,
            pressButtons
        )

        VRTouch.createAndSet(scene, hmd, listOf(TrackerRole.RightHand), false)
        VRTouch.createAndSet(scene, hmd, listOf(TrackerRole.LeftHand), false)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            StageViewerStudyVR(AxonScenario()).main()
//            StageViewerStudyVR(TubeScenario()).main()
        }
    }
}

class StudyFocusMover(val stageSpaceManager: StageSpaceManager, targetJudge: TargetJudge) : RichNode("Focus Mover"),
    VRHandTool {
    private val focusManager = stageSpaceManager.focusManager

    init {
        focusManager.mode = Mode.STEERING

        val offset = Vector3f(0f,100f,0f,)

        update += {
            val positionStageSpace = stageSpaceManager.worldToStageSpace(spatial().worldPosition(), true) + offset
            val coercedPosition =
                stageSpaceManager.hardware.hardwareDimensions().coercePosition(positionStageSpace, null)
            focusManager.focusTarget.spatial().position = coercedPosition
        }

        addAttribute(
            Pressable::class.java, PerButtonPressable(
                mapOf(
                    OpenVRHMD.OpenVRButton.Trigger to SimplePressable(
                        onPress = { _, _ ->
                            targetJudge.hit(focusManager.focusTarget.spatial().position)
                        },
                        onHold = { _, _ ->
                        },
                        onRelease = { _, _ ->
                        })
                )
            )
        )
    }
}

