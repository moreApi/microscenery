package microscenery.scenes.stageStudy

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
import microscenery.*
import microscenery.UI.UIModel
import microscenery.VRUI.Gui3D.Button
import microscenery.VRUI.InHandForwarder
import microscenery.VRUI.VRHandTool
import microscenery.VRUI.VRUIManager
import microscenery.VRUI.behaviors.VR2HandSpatialManipulation
import microscenery.VRUI.behaviors.VRGrabTheWorldSelfMove
import microscenery.scenes.stageStudy.Orchestration.TrialCoordinator
import microscenery.simulation.AxionScenario
import microscenery.simulation.ProceduralBlob
import microscenery.simulation.StageSimulation
import microscenery.stageSpace.FocusManager.Mode
import microscenery.stageSpace.StageSpaceManager
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

        thread {
            Thread.sleep(1000)
            val target = stageSpaceManager.scaleAndRotationPivot.spatial()
            val pivot = stageSpaceManager.stageRoot.spatial().worldPosition(stageSpaceManager.stageAreaCenter)
            val scaleDelta = 0.6f
            // pivot and target are in same space
            for (i in 0..2) {
                target.position.setComponent(i, (target.position[i] + pivot[i] * (scaleDelta - 1)))
            }
            target.scale *= scaleDelta

            target.needsUpdate = true
        }

        var button: Node? = null
        button = Button("ready?") {
            button?.detach()
            logger.warn("Starting!")
            trialCoordinator?.startCase(studyLogger, TrialCoordinator.FinishMessageDisplayer(cam, distance = 0.5f))
            val targetPositions = scenario.generate(stageSpaceManager, stageSimulation.stageSpaceSize)
            val targetBlobs = targetPositions.map {
                val blob = ProceduralBlob(size = 75)
                blob.spatial().position = it
                blob.material().diffuse = Vector3f(0f, .9f, 0f)
                stageSpaceManager.stageRoot.addChild(blob)
                blob
            }
            targetJudge = TargetJudge(targetBlobs, studyLogger, trialCoordinator)
            // init frame movement
            StudyFocusMover(stageSpaceManager, targetJudge).activate(uiModel, TrackerRole.RightHand)
        }.apply {
            this.spatial {
                this.position = stageSpaceManager.stageAreaCenter.copy() + Vector3f(-200f, -100f, 200f)
                this.scale = Vector3f(200f)
            }
            stageSpaceManager.stageRoot.addChild(this)
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
            StageViewerStudyVR(AxionScenario()).main()
            //StageViewerStudyVR(TubeScenario()).main()
        }
    }
}

class StudyFocusMover(val stageSpaceManager: StageSpaceManager, targetJudge: TargetJudge) : RichNode("Focus Mover"),
    VRHandTool {
    private val focusManager = stageSpaceManager.focusManager

    init {
        focusManager.mode = Mode.STEERING

        update += {
            val positionStageSpace = stageSpaceManager.worldToStageSpace(spatial().worldPosition(), true)
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

