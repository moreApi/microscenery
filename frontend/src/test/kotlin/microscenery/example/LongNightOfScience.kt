package microscenery.example

import fromScenery.utils.extensions.plus
import graphics.scenery.controls.behaviours.Action
import graphics.scenery.controls.behaviours.Switch
import graphics.scenery.proteins.Protein
import graphics.scenery.proteins.RibbonDiagram
import graphics.scenery.utils.extensions.times
import graphics.scenery.volumes.*
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


/*fun emdProtein(hub: Hub): Volume {
    //val volume = Volume.fromXML("""C:\Users\JanCasus\volumes\embo\MariaPlant\export.xml""",hub, VolumeViewerOptions())
    val imp: ImagePlus = IJ.openImage("""C:\Users\JanCasus\Downloads\LNDW_VR\emd_3743Stright.tif kept stack.tif""")
    val img: Img<UnsignedShortType> = ImageJFunctions.wrap(imp)


    val volume = Volume.fromRAI(
        img,
        UnsignedShortType(),
        AxisOrder.DEFAULT,
        "Volume loaded with IJ",
        hub,
        VolumeViewerOptions()
    )
    volume.spatial() {
        scale = Vector3f(5f)
    }
    volume.origin = Origin.FrontBottomLeft
    volume.transferFunction = TransferFunction.ramp(0f, 0.8f, 1f)
    volume.colormap = Colormap.get("hot")
    volume.setTransferFunctionRange(0f, 65000f)

    val protein = Protein.fromFile("""C:\Users\JanCasus\Downloads\LNDW_VR\5o3tStright.pdb""")

    val ribbon = RibbonDiagram(protein)

    ribbon.spatial().position = Vector3f(133f,140f,35f)

    volume.addChild(ribbon)

    return volume
}*/


class LongNightOfScience2D : DefaultScene() {

    lateinit var stageSpaceManager: StageSpaceManager
    val msHub = MicrosceneryHub(hub)

    override fun init() {
        super.init()

        cam.spatial().position = Vector3f(0f, 0f, 5f)


        MicroscenerySettings.set("Stage.precisionXY", 1f)
        MicroscenerySettings.set("Stage.precisionZ", 1f)

        val viewSettings = listOf(
            Settings.StageSpace.viewMode,
            Settings.StageSpace.HideFocusFrame,
            Settings.StageSpace.HideFocusTargetFrame,
            Settings.StageSpace.HideStageSpaceLabel
        )
        viewSettings.forEach { MicroscenerySettings.set(it, true) }

        val hw = FileMicroscopeHardware("""C:\Users\JanCasus\Downloads\LNDW_VR\emd_3743Stright.tif kept stack.tif""")
        stageSpaceManager = StageSpaceManager(
            hw,
            scene,
            msHub,
            layout = MicroscopeLayout.Default(MicroscopeLayout.Axis.Z)
//            layout = MicroscopeLayout.Scape(MicroscopeLayout.Axis.Y, 33f)
        )

        stageSpaceManager.stageRoot.spatial().scale *= Vector3f(1f, 1f, 1f)

        stageSpaceManager.sliceManager.transferFunctionManager.maxDisplayRange = 65000f


        val protein = Protein.fromFile("""C:\Users\JanCasus\Downloads\LNDW_VR\5o3tStright.pdb""")
        val ribbon = RibbonDiagram(protein)
        ribbon.spatial().position = Vector3f(0f,0f,25f)

        stageSpaceManager.stageRoot.addChild(ribbon)

        thread {
            while (true) {
                Thread.sleep(500)
                stageSpaceManager
            }
        }

        thread {
            Thread.sleep(1000)
            stageSpaceManager.stack(Vector3f(), Vector3f())
        }

        thread {
            Thread.sleep(5000)
            val cuttingPlane = SlicingPlane()
            cuttingPlane.spatial().rotation = cuttingPlane.spatial().rotation.rotateLocalX(Math.PI.toFloat()/2)
            scene.findByClassname("Volume").firstOrNull()?.let{
                println("found vol")
                if (it !is Volume) return@let
                it.addChild(cuttingPlane)
                cuttingPlane.addTargetVolume(it)
                it.slicingMode = Volume.SlicingMode.Cropping
            }
            while (true){
                Thread.sleep(50)
                val diff = stageSpaceManager.hardware.hardwareDimensions().stageMax
                cuttingPlane.spatial().position = diff.times(1f-(System.currentTimeMillis() % 5000) / 5000f)
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            LongNightOfScience2D().main()
        }
    }
}


class OfflineViewerVRlnos() : DefaultVRScene("Embo Scene") {

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
            WheelMenu(hmd, listOf(Switch("freeze blocks",false){
                vol.volumeManager.freezeRequiredBlocks = it
            },Action("freeze blocks"){
                scene.addChild(croppingTool)
                croppingTool.spatial().position = cam.spatial().worldPosition() + Vector3f(-0.4f, 0f, 0f)
                //croppingTool.activate(vol)
            }), false,), msHub = mshub
        )

        inputHandler?.addBehaviour("cropasd",ClickBehaviour{_,_ ->
            scene.addChild(croppingTool)
            croppingTool.spatial().position = cam.spatial().worldPosition() + Vector3f(-0.4f, 0f, 0f)
        })
        inputHandler?.addKeyBinding("cropasd","P")

    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            OfflineViewerVRlnos().main()
        }
    }
}