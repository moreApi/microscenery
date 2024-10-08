package microscenery.scenes.stageStudy

import fromScenery.utils.extensions.minus
import fromScenery.utils.extensions.plus
import fromScenery.utils.extensions.xyz
import graphics.scenery.Box
import graphics.scenery.volumes.TransferFunction
import microscenery.*
import microscenery.UI.FrameMouseDrag
import microscenery.UI.StageSpaceUI
import microscenery.VRUI.Gui3D.Row
import microscenery.VRUI.Gui3D.TextBox
import microscenery.simulation.ProceduralBlob
import microscenery.simulation.StageSimulation
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector3f
import org.scijava.ui.behaviour.ClickBehaviour
import org.scijava.ui.behaviour.ScrollBehaviour
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.tan
import kotlin.random.Random


class StageViewerStudy2D : DefaultScene(withSwingUI = true, width = 1000, height = 1000) {
    lateinit var stageSpaceManager: StageSpaceManager
    lateinit var stageSimulation: StageSimulation
    val msHub = MicrosceneryHub(hub)

    var currentZLevel = 0
        set(value) {
            val hw = stageSpaceManager.hardware.hardwareDimensions()
            val v = value.toFloat().coerceIn(hw.stageMin.z, hw.stageMax.z).roundToInt()
            updateZ(field,v)
            field = v
        }

    val ZLevelLabel = TextBox(" Z: 0")
    val ZLevelLabelPivot = Row(ZLevelLabel, middleAlign = false)

    lateinit var background: Box

    init {
        MicroscenerySettings.set("Stage.precisionXY", 1f)
        MicroscenerySettings.set("Stage.precisionZ", 1f)
        MicroscenerySettings.set(Settings.UI.ShowSelectionIndicator, false)
        MicroscenerySettings.set(Settings.StageSpace.ShowHullbox, false)
        MicroscenerySettings.set(Settings.StageSpace.ShowStageAreaBorders, false)
        MicroscenerySettings.set(Settings.StageSpace.HideStageSpaceLabel, true)
    }

    override fun init() {
        super.init()
        logger.info("Starting demo hw scene")


        val random = Random(5)
        stageSimulation = StageSimulation(random = random)
        stageSpaceManager = stageSimulation.setupStage(msHub, scene)
        //val targetPositions = StageSimulation.scaffold(stageSpaceManager.stageRoot)
        val targetPositions = stageSimulation.tubeScenario(stageSpaceManager.stageRoot)
        //targetPositions.random().let {
        targetPositions.forEach{
            val blob = ProceduralBlob(size = 75)
            blob.spatial().position = it
            stageSpaceManager.stageRoot.addChild(blob)
        }


        stageSpaceManager.sliceManager.transferFunctionManager.apply {
            this.transferFunction = TransferFunction.flat(1f)
        }


        stageSpaceManager.focusManager.focusTarget.let { focusTarget ->
            var prevZ = stageSpaceManager.focusManager.focusTarget.spatial().worldPosition().z
            focusTarget.lock.withLock {
                focusTarget.update += {
                    val focusZ = focusTarget.spatial().worldPosition().z
                    if (prevZ != focusZ){
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
        background.material().diffuse = Vector3f(0.1f)
        stageSpaceManager.stageRoot.addChild(background)

        cam.spatial().position = Vector3f(0f,0f,1.25f)
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

                position = screenPos + Vector3f(0f, 0f, -0.1f) - cam.position
                scale = Vector3f(0.1f, 0.1f, 0.01f)
            }
        }
        cam.addChild(ZLevelLabelPivot)

        currentZLevel = 0

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
        background.spatial{
            position = Vector3f(0f,0f,new.toFloat()-5f)
            scale = stageSpaceManager.stageAreaBorders.scale.let {
                val v = Vector3f(it)
                v.z = 0.1f
                v
            }
        }

    }

    override fun inputSetup() {
        super.inputSetup()
        StageSpaceUI(stageSpaceManager).stageUI(this, inputHandler, msHub)

        // disable fps camera control
        inputHandler?.addBehaviour("mouse_control", ClickBehaviour{_,_ -> /*dummy*/})

        val frameMouseDrag = FrameMouseDrag(stageSpaceManager.focusManager.focusTarget,{25f})
        inputHandler?.addBehaviour(frameMouseDrag.name,frameMouseDrag)
        inputHandler?.addKeyBinding(frameMouseDrag.name,"button1")
        //inputHandler?.addKeyBinding(frameMouseDrag.name,"scroll")

        inputHandler?.addBehaviour("zScroll", ScrollBehaviour { wheelRotation: Double, isHorizontal: Boolean, x: Int, y: Int ->
            val amount = 10
            val value = currentZLevel + if (wheelRotation > 0) amount else -amount
            val hw = stageSpaceManager.hardware.hardwareDimensions()
            val coerced = value.toFloat().coerceIn(hw.stageMin.z, hw.stageMax.z).roundToInt()
            if (value == coerced) currentZLevel = coerced
            ZLevelLabel.text = "Z: $currentZLevel"
            logger.info("Current : $currentZLevel")
        })
        inputHandler?.addKeyBinding("zScroll","scroll")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            StageViewerStudy2D().main()
        }
    }
}


