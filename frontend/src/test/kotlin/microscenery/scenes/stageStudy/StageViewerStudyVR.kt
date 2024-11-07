package microscenery.scenes.stageStudy

import graphics.scenery.RichNode
import graphics.scenery.SceneryElement
import graphics.scenery.controls.InputHandler
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackerRole
import graphics.scenery.controls.behaviours.*
import graphics.scenery.volumes.TransferFunctionEditor
import microscenery.*
import microscenery.UI.*
import microscenery.VRUI.Gui3D.Button
import microscenery.VRUI.Gui3D.TextBox
import microscenery.VRUI.InHandForwarder
import microscenery.VRUI.VRHandTool
import microscenery.VRUI.VRUIManager
import microscenery.VRUI.behaviors.VR2HandSpatialManipulation
import microscenery.VRUI.behaviors.VRGrabTheWorldSelfMove
import microscenery.VRUI.behaviors.VRTeleport
import microscenery.scenes.stageStudy.Orchestration.TrialCoordinator
import microscenery.simulation.AxionScenario
import microscenery.simulation.ProceduralBlob
import microscenery.simulation.StageSimulation
import microscenery.simulation.StageSimulation.Companion.toggleMaterialRendering
import microscenery.simulation.TubeScenario
import microscenery.stageSpace.FocusManager.Mode
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector3f
import org.scijava.ui.behaviour.ClickBehaviour
import javax.swing.JOptionPane
import kotlin.concurrent.thread
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.system.exitProcess


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
        //cam.spatial().position = Vector3f(0f, -0f, 2f)

        stageSimulation = StageSimulation()
        stageSpaceManager = stageSimulation.setupStage(msHub, scene)

        studyLogger = StudySpatialLogger(cam, msHub,null)

        Button("ready?"){
            trialCoordinator?.startCase(studyLogger)

            val targetPositions = scenario.generate(stageSpaceManager,stageSimulation.stageSpaceSize)
            val targetBlobs = targetPositions.map {
                val blob = ProceduralBlob(size = 75)
                blob.spatial().position = it
                blob.material().diffuse = Vector3f(0f, .9f, 0f)
                stageSpaceManager.stageRoot.addChild(blob)
                blob
            }

            targetJudge = TargetJudge(targetBlobs, studyLogger, trialCoordinator)

            // init frame movement
            uiModel.putInHand(TrackerRole.RightHand, StudyFocusMover(stageSpaceManager,targetJudge))
        }.apply {
            spatial {
                position = stageSpaceManager.stageRoot.spatial().worldPosition(stageSpaceManager.stageAreaCenter)
                scale = Vector3f(0.2f)
            }
        }

    }

    override fun inputSetup() {
        super.inputSetup()
        uiModel = VRUIManager.initUIModel(msHub,hmd)


        VRGrabTheWorldSelfMove.createAndSet(
            scene, hmd, listOf(OpenVRHMD.OpenVRButton.A,OpenVRHMD.OpenVRButton.Menu), listOf(TrackerRole.RightHand, TrackerRole.LeftHand)
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
            //StageViewerStudy3D(AxionScenario()).main()
            StageViewerStudyVR(TubeScenario()).main()
        }
    }
}

class StudyFocusMover(val stageSpaceManager: StageSpaceManager, targetJudge: TargetJudge): RichNode("Focus Mover"), VRHandTool {
    private val focusManager = stageSpaceManager.focusManager

    init {
        focusManager.mode = Mode.STEERING

        update += {
            val positionStageSpace = stageSpaceManager.worldToStageSpace(spatial().position,true)
            val coercedPosition = stageSpaceManager.hardware.hardwareDimensions().coercePosition(positionStageSpace,null)
            focusManager.focusTarget.spatial().position = coercedPosition
        }

        addAttribute(
            Pressable::class.java, PerButtonPressable(
                mapOf(
                    OpenVRHMD.OpenVRButton.Trigger to SimplePressable(
                        onPress = { _,_ ->
                            targetJudge.hit(focusManager.focusTarget.spatial().position)
                        },
                        onHold = { _, _ ->
                        },
                        onRelease = {  _,_ ->
                        })
                )
            )
        )
    }
}

