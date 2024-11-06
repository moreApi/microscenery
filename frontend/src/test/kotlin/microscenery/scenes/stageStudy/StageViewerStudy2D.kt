package microscenery.scenes.stageStudy

import fromScenery.utils.extensions.minus
import fromScenery.utils.extensions.plus
import fromScenery.utils.extensions.xyz
import graphics.scenery.Box
import graphics.scenery.volumes.TransferFunction
import graphics.scenery.volumes.TransferFunctionEditor
import microscenery.*
import microscenery.UI.DesktopUI
import microscenery.UI.FrameMouseDrag
import microscenery.UI.StageSpaceUI
import microscenery.UI.StageUICommand
import microscenery.VRUI.Gui3D.Row
import microscenery.VRUI.Gui3D.TextBox
import microscenery.scenes.stageStudy.Orchestration.TrialCoordinator
import microscenery.simulation.AxionScenario
import microscenery.simulation.ProceduralBlob
import microscenery.simulation.StageSimulation
import microscenery.simulation.StageSimulation.Companion.hideMaterial
import microscenery.simulation.StageSimulation.Companion.showMaterial
import microscenery.simulation.StageSimulation.Companion.toggleMaterialRendering
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector3f
import org.scijava.ui.behaviour.ClickBehaviour
import org.scijava.ui.behaviour.ScrollBehaviour
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.tan
import kotlin.random.Random


class StageViewerStudy2D(val scenario: StageSimulation.Scenario, val trialCoordinator: TrialCoordinator? = null) : DefaultScene(withSwingUI = true, width = 1000, height = 1000) {
    val msHub = MicrosceneryHub(hub)
    lateinit var stageSpaceManager: StageSpaceManager

    lateinit var stageSimulation: StageSimulation
    lateinit var targetJudge: TargetJudge
    lateinit var studyLogger: StudySpatialLogger

    var zInitDone = false
    var currentZLevel = 0
        set(value) {
            if(zInitDone) {
                val hw = stageSpaceManager.hardware.hardwareDimensions()
                val v = value.toFloat().coerceIn(hw.stageMin.z, hw.stageMax.z).roundToInt()
                updateZ(field, v)
                field = v
            } else {
                field = value
            }
        }

    val ZLevelLabel = TextBox(" Z: 0")
    val ZLevelLabelPivot = Row(ZLevelLabel, middleAlign = false)

    var background: Box? = null

    init {
        MicroscenerySettings.set("Stage.precisionXY", 1f)
        MicroscenerySettings.set("Stage.precisionZ", 1f)
        MicroscenerySettings.set(Settings.UI.ShowSelectionIndicator, false)
        MicroscenerySettings.set(Settings.StageSpace.ShowHullbox, false)
        MicroscenerySettings.set(Settings.StageSpace.ShowStageAreaBorders, false)
        MicroscenerySettings.set(Settings.StageSpace.HideStageSpaceLabel, true)
        MicroscenerySettings.set(Settings.StageSpace.ShowFocusPositionLabel, false)
    }

    override fun init() {
        super.init()
        logger.info("Starting demo hw scene")

        val seed = Random.nextInt()
        val random = Random(seed)
        println("Seed: $seed")
        stageSimulation = StageSimulation(random = random)
        stageSpaceManager = stageSimulation.setupStage(msHub, scene)

        val targetPositions = scenario.generate(stageSpaceManager,stageSimulation.stageSpaceSize)
        val targetBlobs = targetPositions.map {
            val blob = ProceduralBlob(size = 75)
            blob.spatial().position = it
            blob.material().diffuse = Vector3f(0f, .9f, 0f)
            stageSpaceManager.stageRoot.addChild(blob)
            blob
        }

        studyLogger = StudySpatialLogger(cam, msHub,null)

        targetJudge = TargetJudge(targetBlobs, studyLogger, trialCoordinator)

        stageSpaceManager.sliceManager.transferFunctionManager.apply {
            this.transferFunction = TransferFunction.flat(1f)
        }

        currentZLevel = stageSpaceManager.stagePosition.z.toInt()
        ZLevelLabel.text = "Z: $currentZLevel"
        zInitDone = true

        stageSpaceManager.focusManager.focusTarget.let { focusTarget ->
            var prevZ = stageSpaceManager.focusManager.focusTarget.spatial().worldPosition().z
            focusTarget.lock.withLock {
                focusTarget.update += {
                    val focusZ = focusTarget.spatial().worldPosition().z
                    if (prevZ != focusZ) {
                        val diff = focusZ - prevZ
                        cam.lock.withLock {
                            cam.spatial {
                                position.z += diff
                                needsUpdate = true
                            }
                        }
                        prevZ = focusZ
                    }

                }
            }
        }

        background = Box(Vector3f(1f))
        background?.material()?.diffuse = Vector3f(0.1f)
        stageSpaceManager.stageRoot.addChild(background!!)
        updateZ(currentZLevel,currentZLevel)

        cam.spatial().position = Vector3f(0f, 0f, 1.25f)
        cam.update += {
            ZLevelLabelPivot.spatial {
                val x = 5
                val y = cam.height * 0.1f
                // calculate aspect ratio, note here that both width and height
                // are integers and need to be converted before the division, otherwise
                // we end up with an incorrect (integer) result
                val aspect: Float = cam.width.toFloat() / cam.height.toFloat()
                val tanFov = tan(cam.fov / 2.0f * PI.toFloat() / 180.0f)

                // shift the x and y coordinates to a [-1, 1] coordinate system,
                // with 0,0 being center
                val posX = (2.0f * ((x + 0.5f) / cam.width) - 1) * tanFov * aspect
                val posY = (1.0f - 2.0f * ((y + 0.5f) / cam.height)) * tanFov

                val screenPos = cam.spatial().viewToWorld(
                    Vector3f(
                        posX, posY,
                        -1.0f
                    )
                ).xyz()

                position = screenPos + Vector3f(0f, 0f, -0.1f) - cam.spatial().position
                scale = Vector3f(0.1f, 0.1f, 0.01f)
            }
        }
        cam.addChild(ZLevelLabelPivot)

        thread {
            while (true) {
                Thread.sleep(200)
                scene to stageSpaceManager
            }
        }
    }


    private fun updateZ(old: Int, new: Int) {
        val diff = old - new
        stageSpaceManager.focusManager.focusTarget.let { focusTarget ->
            focusTarget.spatial {
                position.z += -diff
                needsUpdate = true
            }
        }

        stageSpaceManager.sliceManager.let { sm ->
            sm.sortingSlicesLock.withLock {
                sm.sortedSlices.forEach { slice ->
                    slice.visible = slice.spatial().position.z.roundToInt() == new
                }
            }
        }
        targetJudge.targets.forEach { blob, isHit ->
            if (!isHit) return@forEach

            if ((blob.spatial().position.z - new).absoluteValue < blob.radius*1.1f){
                blob.showMaterial()
            } else {
                blob.hideMaterial()
            }

        }

        background?.spatial {
            position = Vector3f(stageSpaceManager.stageAreaCenter.x, stageSpaceManager.stageAreaCenter.y, new.toFloat() - 5f)
            scale = stageSpaceManager.stageAreaBorders.spatial().scale.let {
                val v = Vector3f(it)
                v.z = 0.1f
                v
            }
        }

        studyLogger.logEvent("scrollZ", listOf(currentZLevel.toString()))

    }

    override fun inputSetup() {
        super.inputSetup()
        val ssUI = StageSpaceUI(stageSpaceManager)
        //ssUI.stageUI(this, inputHandler, msHub)
        val commands = listOf(ssUI.comStackAcq, ssUI.comStop, ssUI.comSteering, ssUI.comGoLive,
            StageUICommand("toggle material", null, object : ClickBehaviour {
                override fun click(p0: Int, p1: Int) {
                    scene.toggleMaterialRendering()
                }
            }), StageUICommand("mark RoI", "button3", object : ClickBehaviour {
                override fun click(p0: Int, p1: Int) {
                    targetJudge.hit(stageSpaceManager.focusManager.focusTarget.spatial().position)
                }
            }), StageUICommand("transfer function", null, object : ClickBehaviour {
                override fun click(p0: Int, p1: Int) {
                    TransferFunctionEditor.showTFFrame(stageSpaceManager.sliceManager.transferFunctionManager)
                }
            })
        )

        this.extraPanel?.let { ssUI.stageSwingUI(it, msHub, commands) }
        this.mainFrame?.pack()
        DesktopUI.initMouseSelection(inputHandler, msHub)

        inputHandler?.let { inputHandler ->
            ssUI.stageKeyUI(inputHandler, cam, commands)
        }

        // disable fps camera control
        inputHandler?.addBehaviour("mouse_control", ClickBehaviour { _, _ -> /*dummy*/ })

        val frameMouseDrag = FrameMouseDrag(stageSpaceManager.focusManager.focusTarget, cam, { 25f })
        inputHandler?.addBehaviour(frameMouseDrag.name, frameMouseDrag)
        inputHandler?.addKeyBinding(frameMouseDrag.name, "button1")
        //inputHandler?.addKeyBinding(frameMouseDrag.name,"scroll")

        inputHandler?.addBehaviour(
            "zScroll",
            ScrollBehaviour { wheelRotation: Double, isHorizontal: Boolean, x: Int, y: Int ->
                val amount = 10
                val value = currentZLevel + if (wheelRotation > 0) amount else -amount
                val hw = stageSpaceManager.hardware.hardwareDimensions()
                val coerced = value.toFloat().coerceIn(hw.stageMin.z, hw.stageMax.z).roundToInt()
                if (value == coerced) currentZLevel = coerced
                ZLevelLabel.text = "Z: $currentZLevel"
                logger.info("Current : $currentZLevel")
            })
        inputHandler?.addKeyBinding("zScroll", "scroll")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            StageViewerStudy2D(AxionScenario()).main()
        }
    }
}


