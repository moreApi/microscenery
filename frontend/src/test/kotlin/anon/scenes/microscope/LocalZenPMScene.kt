package anon.scenes.microscope

import graphics.scenery.Light
import graphics.scenery.utils.Wiggler
import graphics.scenery.volumes.Colormap
import anon.DefaultScene
import anon.MicrosceneryHub
import anon.MicroscenerySettings
import anon.Settings
import anon.UI.CrossRayToPosBehavior
import anon.UI.StageSpaceUI
import anon.UI.StageUICommand
import anon.VRUI.PointCloudAblationTool.Ink
import anon.hardware.MicroscopeHardware
import anon.simulation.AblationSimulationMicroscope
import anon.stageSpace.StageSpaceManager
import anon.zenSysConCon.ZenBlueTCPConnector
import anon.zenSysConCon.ZenPhotoMnplMicroscope
import org.joml.Vector3f
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import java.io.File
import kotlin.concurrent.thread

@Suppress("unused")
class LocalZenPMScene : DefaultScene(withSwingUI = true) {
    lateinit var  stageSpaceManager: StageSpaceManager
    val msHub = MicrosceneryHub(hub)

    val volumesFolder = """D:\volumes"""
    val zeissFolder = """D:\ViingsCloud\Zeiss"""

    lateinit var zenMicroscope: ZenPhotoMnplMicroscope
    val crovWithoutHoles = volumesFolder + """\Zeiss\20230915_488_corvoria_without_holes.czi"""
    val singleCZI = """D:\volumes\20250305_arthur_3d_ablation\20250305_MAmCherry_-15-1.czi\20250305_MAmCherry_-15-1_AcquisitionBlock4.czi\20250305_MAmCherry_-15-1_AcquisitionBlock4.czi"""
    val mbCZI = """D:\volumes\20250305_arthur_3d_ablation\20250305_MAmCherry_-15-1.czi"""
    val initalExperimentFile = """zenSysConCon/src/test/resources/20250305_3D_laser_ablation_settings_test_1_shorted.czexp"""

    val drosphila_cut = listOf("""D:\volumes\Zeiss\20230925_drosophila_niceSideCut1.czi""","""D:\volumes\Zeiss\20230925_drosophila_niceSideCut2.czi""")

    override fun init() {
        super.init()
        MicroscenerySettings.set(Settings.StageSpace.HideFocusFrame,true)
        MicroscenerySettings.set(Settings.StageSpace.HideFocusTargetFrame,true)
        MicroscenerySettings.set(Settings.ZenMicroscope.readChannel,1)
        MicroscenerySettings.set(Settings.Ablation.SizeUM,30f)

        cam.spatial().position = Vector3f(0f, 0f, 2f)

        val zenBlue: ZenBlueTCPConnector = let{
            if (MicroscenerySettings.get(Settings.ZenMicroscope.MockZenConnection, true)) {
                val tmp = Mockito.mock(ZenBlueTCPConnector::class.java)
                whenever(tmp.saveExperimentAndGetFilePath()).thenReturn(initalExperimentFile)
                whenever(tmp.getCurrentDocument()).thenReturn(singleCZI)
                //whenever(tmp.getCurrentDocument()).thenReturn(drosphila_cut[0],drosphila_cut[1])
                tmp
            } else {
                ZenBlueTCPConnector()
            }
        }

        zenMicroscope = ZenPhotoMnplMicroscope(zenBlue)

        val hardware: MicroscopeHardware = AblationSimulationMicroscope(zenMicroscope)
        stageSpaceManager = StageSpaceManager(hardware, scene, msHub)

        //for nice cut pictures
        stageSpaceManager.scene.findByClassname("Light").forEach { (it as Light).intensity *= 0.25f }
        stageSpaceManager.sliceManager.stacks.firstOrNull()?.volume?.colormap = Colormap.get("grays")

        thread {
            Thread.sleep(100)
            zenMicroscope.sync()
            zenMicroscope.snapSlice()
            //stageSpaceManager.sliceManager.transferFunctionManager.loadTransferFunctionFromFile(File("""C:\Users\JanPhD\Downloads\drosophila.tf"""))
            stageSpaceManager.sliceManager.transferFunctionManager.loadTransferFunctionFromFile(File("""C:\Users\JanPhD\Downloads\MamCherryArthur.tf"""))
            stageSpaceManager.focusManager.focusTarget.spatial().position = stageSpaceManager.stageAreaCenter
        }

        thread {
            while (true){
                Thread.sleep(200)
                stageSpaceManager to zenMicroscope
            }
        }
    }

    fun placeAblationInk(pos: Vector3f){
        val pointColor = Vector3f(1f)
        val ink = Ink(MicroscenerySettings.get(Settings.Ablation.SizeUM, 8f) * 0.5f, pointColor, null)
        ink.spatial().let {
            it.position = pos
        }
        scene.addChild(ink)
        ink.spatial().updateWorld(false,true)

        val posInStageSpace = stageSpaceManager.worldToStageSpace(ink.spatial().worldPosition())
        val coerced = stageSpaceManager.hardware.hardwareDimensions().coercePosition(posInStageSpace, null, true)

        if (posInStageSpace != coerced) {
            //ink is out of stage space bounds, wiggle in protest
            Wiggler(ink, 0.01f, 300)
            scene.removeChild(ink)
            return
        }

        //ink.spatial().position = ink.spatial().worldPosition()
        stageSpaceManager.worldToStageSpace(ink.spatial())
        ink.spatial().scale = Vector3f(1f)

        scene.removeChild(ink)
        stageSpaceManager.stageRoot.addChild(ink)
    }

    override fun inputSetup() {
        super.inputSetup()
        StageSpaceUI(stageSpaceManager).stageUI(this,inputHandler, msHub, listOf(
            StageUICommand("load Stack 1",null){_,_ ->
                zenMicroscope.debugStack(this.singleCZI)
            },
            StageUICommand("load Stack 2",null){_,_ ->
                zenMicroscope.debugStack(this.crovWithoutHoles)
            },
            StageUICommand("ablateZen compose",null){_,_ ->
                stageSpaceManager.ablationManager.composeAblation()
            },
            StageUICommand("ablateZen scrap",null){_,_ ->
                stageSpaceManager.ablationManager.scrapAblation()
            },
            StageUICommand("ablateZen execute",null){_,_ ->
                stageSpaceManager.ablationManager.executeAblation()
            }
        ))


        val crossRay = CrossRayToPosBehavior(scene.activeObserver!!){
            placeAblationInk(it)
        }
        inputHandler!!.addBehaviour("crossRay", crossRay)
        inputHandler!!.addKeyBinding("crossRay","G")

    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            LocalZenPMScene().main()
        }
    }
}