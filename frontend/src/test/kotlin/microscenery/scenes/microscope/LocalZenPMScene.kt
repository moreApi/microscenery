package microscenery.scenes.microscope

import graphics.scenery.Light
import graphics.scenery.utils.Wiggler
import graphics.scenery.volumes.Colormap
import microscenery.*
import microscenery.UI.CrossRayToPosBehavior
import microscenery.UI.StageSpaceUI
import microscenery.UI.StageUICommand
import microscenery.VRUI.PointCloudAblationTool.Ink
import microscenery.hardware.MicroscopeHardware
import microscenery.stageSpace.StageSpaceManager
import microscenery.zenSysConCon.ZenBlueTCPConnector
import microscenery.zenSysConCon.ZenPhotoMnplMicroscope
import org.joml.Vector3f
import org.mockito.Mockito
import org.mockito.kotlin.whenever
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

    override fun init() {
        super.init()
        MicroscenerySettings.set(Settings.StageSpace.HideFocusFrame,true)
        MicroscenerySettings.set(Settings.StageSpace.HideFocusTargetFrame,true)
        MicroscenerySettings.set(Settings.ZenMicroscope.readChannel,1)

        cam.spatial().position = Vector3f(0f, 0f, 5f)

        val zenBlue: ZenBlueTCPConnector = let{
            if (MicroscenerySettings.get(Settings.ZenMicroscope.MockZenConnection, true)) {
                val tmp = Mockito.mock(ZenBlueTCPConnector::class.java)
                whenever(tmp.saveExperimentAndGetFilePath()).thenReturn(initalExperimentFile)
                whenever(tmp.getCurrentDocument()).thenReturn(crovWithoutHoles)
                tmp
            } else {
                ZenBlueTCPConnector()
            }
        }

        zenMicroscope = ZenPhotoMnplMicroscope(zenBlue)

        val hardware: MicroscopeHardware = zenMicroscope
        stageSpaceManager = StageSpaceManager(hardware, scene, msHub)

        //for nice cut pictures
        stageSpaceManager.scene.findByClassname("Light").forEach { (it as Light).intensity *= 0.25f }
        stageSpaceManager.sliceManager.stacks.firstOrNull()?.volume?.colormap = Colormap.get("grays")

        thread {
            Thread.sleep(100)
            zenMicroscope.sync()
            zenMicroscope.snapSlice()
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