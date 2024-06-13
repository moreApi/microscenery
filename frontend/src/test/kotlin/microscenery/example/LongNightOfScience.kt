package microscenery.example

import graphics.scenery.Box
import graphics.scenery.Hub
import graphics.scenery.attribute.material.Material
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackerRole
import graphics.scenery.primitives.Atmosphere
import graphics.scenery.proteins.Protein
import graphics.scenery.proteins.RibbonDiagram
import graphics.scenery.volumes.Volume
import microscenery.*
import microscenery.UI.StageSpaceUI
import microscenery.VRUI.CroppingTool
import microscenery.VRUI.Gui3D.Column
import microscenery.VRUI.Gui3D.Row
import microscenery.VRUI.Gui3D.TextBox
import microscenery.VRUI.VRUIManager
import microscenery.stageSpace.MicroscopeLayout
import microscenery.stageSpace.StageSpaceManager
import org.joml.Quaternionf
import org.joml.Vector3f
import org.scijava.ui.behaviour.ClickBehaviour
import kotlin.concurrent.thread


class LongNightOfScience2D : DefaultScene("Loooooooong night of science", VR = false, width = 840, height = 840) {

    val atmosphere = false

    init {
        val viewSettings = listOf(
            Settings.StageSpace.viewMode,
            Settings.StageSpace.HideFocusFrame,
            Settings.StageSpace.HideFocusTargetFrame,
            Settings.StageSpace.HideStageSpaceLabel
        )
        viewSettings.forEach { MicroscenerySettings.set(it, true) }
        MicroscenerySettings.set(Settings.StageSpace.ShowHullbox, !atmosphere)
    }

    class ProteinFiles(val name: String, val tif: String?, val pdb: String, val pdbOffset: Vector3f)

    val pFiles = listOf(
        ProteinFiles(
            "5o3tStright",
            """C:\Users\JanCasus\Downloads\LNDW_VR\emd_3743Stright.tif kept stack.tif""",
            """C:\Users\JanCasus\Downloads\LNDW_VR\5o3tStright.pdb""",
            Vector3f(0f, 5f, 25f)
        ),
        ProteinFiles(
            "5o3lPaired",
            """C:\Users\JanCasus\Downloads\LNDW_VR\emd_3741Paired.tif kept stack.tif""",
            """C:\Users\JanCasus\Downloads\LNDW_VR\5o3lPaired.pdb""",
            Vector3f(20f, 0f, 37f)
        ),
        ProteinFiles(
            "6qi6 beta lactoglobulin",
            null,
            """C:\Users\JanCasus\Downloads\LNDW_VR\6qi6_beta_lactoglobulin.pdb""",
            Vector3f(0f, 0f, 0f)
        ),
        ProteinFiles(
            "AF-P02666 beta casein",
            null,
            """C:\Users\JanCasus\Downloads\LNDW_VR\AF-P02666_beta_casein.pdb""",
            Vector3f(0f, 0f, 0f)
        ),
    )

    var mainProtein: RibbonDiagram? = null

    lateinit var stageSpaceManager: StageSpaceManager
    val msHub = MicrosceneryHub(hub)
    val microscope = FileMicroscopeHardware(pFiles.first().tif!!)

    val mainLabel = TextBox("A Protein")

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
        cam.spatial().position = Vector3f(0f, if (VR) -1f else 0f, 2f)

        stageSpaceManager = StageSpaceManager(
            microscope,
            scene,
            msHub,
            layout = MicroscopeLayout.Default(MicroscopeLayout.Axis.Z)
//            layout = MicroscopeLayout.Scape(MicroscopeLayout.Axis.Y, 33f)
        )

        stageSpaceManager.sliceManager.transferFunctionManager.maxDisplayRange = 65000f


        val column = Column(
            Row(mainLabel)
        )

        column.spatial {
            scale = Vector3f(0.1f)

            // follow head/cam rotation
            fun updateMSLabelRotation(viewOrientation: Quaternionf) {
                rotation = Quaternionf(viewOrientation).conjugate().normalize()
            }
            val hmd = msHub.getAttribute(Hub::class.java).get<OpenVRHMD>()

            column.update += {
                if (hmd != null) {
                    updateMSLabelRotation(hmd.getOrientation())
                } else {
                    scene.activeObserver?.let { updateMSLabelRotation(it.spatial().rotation) }
                }

                // move it on top of the stage space
                val centerW = stageSpaceManager.stageRoot.spatial().worldPosition(stageSpaceManager.stageAreaCenter)
                centerW.y = stageSpaceManager.stageAreaBorders.generateBoundingBox()?.asWorld()?.max?.y ?: centerW.y
                centerW.y += 0.1f
                position = centerW
            }
        }
        scene.addChild(column)
        column.pack()


        setProteinActive(0)
        setupSecondaryProteins()

        thread {
            while (true) {
                Thread.sleep(200)
                stageSpaceManager
            }
        }

//        thread {
//            Thread.sleep(5000)
//            val cuttingPlane = SlicingPlane()
//            cuttingPlane.spatial().rotation = cuttingPlane.spatial().rotation.rotateLocalX(Math.PI.toFloat()/2)
//            scene.findByClassname("Volume").firstOrNull()?.let{
//                println("found vol")
//                if (it !is Volume) return@let
//                it.addChild(cuttingPlane)
//                cuttingPlane.addTargetVolume(it)
//                it.slicingMode = Volume.SlicingMode.Cropping
//            }
//            while (true){
//                Thread.sleep(50)
//                val diff = stageSpaceManager.hardware.hardwareDimensions().stageMax
//                cuttingPlane.spatial().position = diff.times(1f-(System.currentTimeMillis() % 5000) / 5000f)
//            }
//        }
    }

    private fun setupSecondaryProteins() {
        val p1 = RibbonDiagram(Protein.fromFile(pFiles[2].pdb))
        val p2 = RibbonDiagram(Protein.fromFile(pFiles[3].pdb))
        val p1Label = TextBox(pFiles[2].name)
        val p2Label = TextBox(pFiles[3].name)
        p1.spatial {
            scale = Vector3f(0.04f)
            position = Vector3f(2f, 0f, 2f)
        }
        scene.addChild(p1)
        p1Label.spatial{
            scale = Vector3f(0.1f)
            rotation = Quaternionf().rotateY(-Math.PI.toFloat()/2)
            position = Vector3f(2f, 0.5f, 1.75f)
        }
        scene.addChild(p1Label)
        p2.spatial {
            scale = Vector3f(0.01f)
            position = Vector3f(-2f, 0f, 2f)
        }
        scene.addChild(p2)
        p2Label.spatial{
            scale = Vector3f(0.1f)
            rotation = Quaternionf().rotateY(Math.PI.toFloat()/2)
            position = Vector3f(-2f, 0.5f, 2.25f)
        }
        scene.addChild(p2Label)

        listOf(p1, p2).forEach {
            val lastUpdate = System.currentTimeMillis()
            val rotationMS = 20000
            it.postUpdate += {
                val now = System.currentTimeMillis()
                val delta = now - lastUpdate
                it.spatial().rotation.rotationY((2 * Math.PI * (delta % rotationMS / rotationMS.toFloat())).toFloat())
                it.spatial().needsUpdate = true
            }
        }
    }

    private fun setProteinActive(index: Int) {
        val p = pFiles.getOrNull(index) ?: return

        cam.showMessage2("loading ${p.name}")

        stageSpaceManager.clearStage()
        p.tif?.let {
            microscope.loadImg(it)
            stageSpaceManager.stack(Vector3f(), Vector3f())
            // workaround activate cropping tool for new volume
            val cp = scene.findByClassname(CroppingTool::class.simpleName!!).firstOrNull() as? CroppingTool
            val vol = scene.findByClassname("Volume").firstOrNull() as? Volume
            if (cp != null && vol != null) {
                cp.activate(vol)
            }
        }

        mainProtein?.detach()
        val protein = Protein.fromFile(p.pdb)
        mainProtein = RibbonDiagram(protein)
        mainProtein!!.spatial().position = p.pdbOffset
        stageSpaceManager.stageRoot.addChild(mainProtein!!)

        mainLabel.text = p.name
    }

    override fun inputSetup() {
        super.inputSetup()
        // protein selectors
        pFiles.forEachIndexed { index, pf ->
            val name = "activate${pf.name}"
            inputHandler?.addBehaviour(name, ClickBehaviour { _, _ ->
                setProteinActive(index)
            })
            inputHandler?.addKeyBinding(name, (index + 1).toString())
        }

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

        val disabled: MutableList<Pair<String, String>> = mutableListOf()
        inputHandler?.addBehaviour("toggleVrControl", ClickBehaviour { _, _ ->

            if (disabled.isEmpty()) {
                val buttons = listOf(
                    OpenVRHMD.OpenVRButton.System,
                    OpenVRHMD.OpenVRButton.Menu,
                    OpenVRHMD.OpenVRButton.A,
                    //OpenVRHMD.OpenVRButton.Side,
                    OpenVRHMD.OpenVRButton.Trigger
                )
                hmd.getAllBindings().forEach { (inputTrigger, behaviors) ->
                    buttons.flatMap {
                        listOf(TrackerRole.LeftHand to it, TrackerRole.RightHand to it)
                    }.map {
                        OpenVRHMD.keyBinding(it.first, it.second)
                    }.forEach { buttonToDisable ->
                        if (buttonToDisable == inputTrigger.toString()) {
                            behaviors.forEach {
                                disabled.add(buttonToDisable to it)
                                hmd.removeKeyBinding2(it)
                                logger.info("disabled $it :$buttonToDisable")
                            }
                        }
                    }
                }
            } else {
                disabled.forEach {
                    hmd.addKeyBinding(it.second, it.first)
                    logger.info("activated $it")
                }
                disabled.clear()
            }

        })
        inputHandler?.addKeyBinding("toggleVrControl", "O")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            LongNightOfScience2D().main()
        }
    }
}




