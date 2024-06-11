package microscenery.example

import fromScenery.utils.extensions.plus
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackerRole
import graphics.scenery.controls.behaviours.Action
import graphics.scenery.controls.behaviours.Switch
import graphics.scenery.proteins.Protein
import graphics.scenery.proteins.RibbonDiagram
import graphics.scenery.volumes.TransferFunctionEditor
import graphics.scenery.volumes.Volume
import microscenery.*
import microscenery.UI.UIModel
import microscenery.VRUI.CroppingTool
import microscenery.VRUI.VRUIManager
import microscenery.VRUI.fromScenery.WheelMenu
import microscenery.stageSpace.MicroscopeLayout
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector3f
import org.scijava.ui.behaviour.ClickBehaviour
import kotlin.concurrent.thread


class LongNightOfScience2D : DefaultScene("Loooooooong night of science") {

    class ProteinFiles(val name: String, val tif: String?, val pdb: String, val pdbOffset: Vector3f)

    val pFiles = listOf(
        ProteinFiles(
            "5o3tStright",
            """C:\Users\JanCasus\Downloads\LNDW_VR\emd_3743Stright.tif kept stack.tif""",
            """C:\Users\JanCasus\Downloads\LNDW_VR\5o3tStright.pdb""",
            Vector3f(0f, 0f, 25f)
        ),
        ProteinFiles(
            "5o3lPaired",
            """C:\Users\JanCasus\Downloads\LNDW_VR\emd_3741Paired.tif kept stack.tif""",
            """C:\Users\JanCasus\Downloads\LNDW_VR\5o3lPaired.pdb""",
            Vector3f(0f, 0f, 45f)
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

    lateinit var stageSpaceManager: StageSpaceManager
    val msHub = MicrosceneryHub(hub)
    val microscope = FileMicroscopeHardware(pFiles.first().tif!!)

    override fun init() {
        super.init()

        cam.spatial().position = Vector3f(0f, 0f, 2f)

        val viewSettings = listOf(
            Settings.StageSpace.viewMode,
            Settings.StageSpace.HideFocusFrame,
            Settings.StageSpace.HideFocusTargetFrame,
            Settings.StageSpace.HideStageSpaceLabel
        )
        viewSettings.forEach { MicroscenerySettings.set(it, true) }

        stageSpaceManager = StageSpaceManager(
            microscope,
            scene,
            msHub,
            layout = MicroscopeLayout.Default(MicroscopeLayout.Axis.Z)
//            layout = MicroscopeLayout.Scape(MicroscopeLayout.Axis.Y, 33f)
        )

        stageSpaceManager.sliceManager.transferFunctionManager.maxDisplayRange = 65000f

        setProteinActive(0)

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

    private fun setProteinActive(index: Int) {
        val p = pFiles.getOrNull(index) ?: return

        cam.showMessage2("loading ${p.name}")

        stageSpaceManager.clearStage()
        p.tif?.let {
            microscope.loadImg(it)
            stageSpaceManager.stack(Vector3f(), Vector3f())
        }

        scene.findByClassname("RibbonDiagram").firstOrNull()?.detach()
        val protein = Protein.fromFile(p.pdb)
        val ribbon = RibbonDiagram(protein)
        ribbon.spatial().position = p.pdbOffset
        stageSpaceManager.stageRoot.addChild(ribbon)

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
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            LongNightOfScience2D().main()
        }
    }
}


class OfflineViewerVRlnos : DefaultVRScene("Embo Scene") {

    lateinit var vol: Volume

    val mshub = MicrosceneryHub(hub)
    val croppingTool = CroppingTool(mshub.getAttribute(UIModel::class.java))

    override fun init() {
        super.init()


        thread {
            //delay volume loading to not crash VR...
            Thread.sleep(1000)
            croppingTool.volume = vol
            //vol.slicingMode = Volume.SlicingMode.Slicing
            scene.addChild(vol)
            TransferFunctionEditor.showTFFrame(vol)
        }

        thread {
            // debug loop
            //while (true) {
            //    Thread.sleep(500)
            //   val s = vol
            //}
        }
    }

    override fun inputSetup() {
        super.inputSetup()

        //mshub.getAttribute(UIModel::class.java).selected = vol
        val mshub = MicrosceneryHub(hub)
        val croppingTool = CroppingTool(mshub.getAttribute(UIModel::class.java))


        VRUIManager.initBehavior(
            scene, hmd, inputHandler, customActions =
            WheelMenu(
                hmd,
                listOf(Switch("freeze blocks", false) {
                    vol.volumeManager.freezeRequiredBlocks = it
                }, Action("freeze blocks") {
                    scene.addChild(croppingTool)
                    croppingTool.spatial().position = cam.spatial().worldPosition() + Vector3f(-0.4f, 0f, 0f)
                    //croppingTool.activate(vol)
                }),
                false,
            ), msHub = mshub
        )

        val disabled: MutableList<Pair<String, String>> = mutableListOf()
        inputHandler?.addBehaviour("toggleVrControl", ClickBehaviour { _, _ ->

            if (disabled.isEmpty()) {
                val buttons = listOf(
                    OpenVRHMD.OpenVRButton.System,
                    OpenVRHMD.OpenVRButton.Menu,
                    OpenVRHMD.OpenVRButton.A,
                    OpenVRHMD.OpenVRButton.Side,
                    OpenVRHMD.OpenVRButton.Trigger
                )
                inputHandler?.getAllBindings()?.forEach { t, u ->
                    buttons.flatMap {
                        listOf(TrackerRole.LeftHand to it, TrackerRole.RightHand to it)
                    }.map {
                        OpenVRHMD.keyBinding(it.first, it.second)
                    }.forEach { bindingName ->
                        if (bindingName == t.toString()) {
                            u.forEach {
                                disabled.add(bindingName to it)
                                inputHandler?.removeKeyBinding(it)
                                logger.info("disabled $it :$bindingName")
                            }
                        }
                    }
                }
            } else {
                disabled.forEach {
                    inputHandler?.addKeyBinding(it.second, it.first)
                    logger.info("activated $it")
                }
                disabled.clear()
            }

        })
        inputHandler?.addKeyBinding("toggleVrControl", "P")

    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            OfflineViewerVRlnos().main()
        }
    }
}