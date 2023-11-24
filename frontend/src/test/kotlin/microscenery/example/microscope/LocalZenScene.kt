package microscenery.example.microscope

import graphics.scenery.Light
import graphics.scenery.volumes.Colormap
import microscenery.*
import microscenery.UI.StageSpaceUI
import microscenery.UI.StageUICommand
import microscenery.hardware.MicroscopeHardware
import microscenery.stageSpace.StageSpaceManager
import microscenery.zenSysConCon.ZenBlueTCPConnector
import microscenery.zenSysConCon.ZenMicroscope
import microscenery.zenSysConCon.sysCon.SysConConnection
import org.joml.Vector3f
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import kotlin.concurrent.thread

@Suppress("unused")
class LocalZenScene : DefaultScene(withSwingUI = true) {
    val stageSpaceManager: StageSpaceManager
    val msHub = MicrosceneryHub(hub)

    val zenMicroscope: ZenMicroscope
    val crovWithoutHoles = """C:\Users\JanCasus\volumes\Zeiss\20230915_488_corvoria_without_holes.czi"""
    val crovHoles = """C:\Users\JanCasus\volumes\Zeiss\20230915_488_corvoria_with_holes.czi"""
    val experiment19 = """C:\Users\JanCasus\Zeiss\Experiment-19.czi"""
    val squareRing = """C:\Nextcloud\Zeiss\sd3\20230712_488_square_ring.czi"""
    val squareRing2 = """C:\Nextcloud\Zeiss\sd3\20230712_488_square_ring2.czi"""
    val squareRing3 = """C:\Nextcloud\Zeiss\sd3\20230712_488_square_ring3.czi"""
    val sd3Fly = """C:\Users\JanCasus\volumes\Zeiss\marina-sd3-drosophila1.czi"""
    val niceCut1 = """C:\Users\JanCasus\volumes\Zeiss\20230925_drosophila_niceSideCut1.czi"""

    init {
        MicroscenerySettings.set(Settings.StageSpace.HideFocusFrame,true)
        MicroscenerySettings.set(Settings.StageSpace.HideFocusTargetFrame,true)

        cam.spatial().position = Vector3f(0f, 0f, 5f)



        val zenBlue: ZenBlueTCPConnector = Mockito.mock(ZenBlueTCPConnector::class.java)
        val sysCon: SysConConnection = Mockito.mock(SysConConnection::class.java)
        whenever(zenBlue.getCurrentDocument()).thenReturn(squareRing)
        zenMicroscope = ZenMicroscope(zenBlue, sysCon)

        val hardware: MicroscopeHardware = zenMicroscope
        stageSpaceManager = StageSpaceManager(hardware, scene, msHub)

        //for nice cut pictures
        stageSpaceManager.scene.findByClassname("Light").forEach { (it as Light).intensity *= 0.25f }
        stageSpaceManager.sliceManager.stacks.firstOrNull()?.volume?.colormap = Colormap.get("grays")

        thread {
            Thread.sleep(100)
            val l1 = niceCut1
            logger.info("init $l1")
            zenMicroscope.debugStack(l1)

            Thread.sleep(3000)
            val l2 = sd3Fly
            logger.info("init $l2")
            //zenMicroscope.debugStack(l2)
        }
        thread {
            while (true){
                Thread.sleep(200)
                stageSpaceManager to zenMicroscope
            }
        }
    }

    override fun inputSetup() {
        super.inputSetup()
        StageSpaceUI(stageSpaceManager).stageUI(this,inputHandler, msHub, listOf(
            StageUICommand("next Vol",null){
                    _: Int, _: Int ->
                stageSpaceManager.sliceManager.selectedStack?.volume?.let { vol ->
                    val goto = vol.nextTimepoint()
                    scene.findObserver()?.showMessage2("Timepoint $goto", duration = 1500)
                }
            },
            StageUICommand("prev Vol",null){
                    _: Int, _: Int ->
                stageSpaceManager.sliceManager.selectedStack?.volume?.let { vol ->
                    val goto = vol.previousTimepoint()
                    scene.findObserver()?.showMessage2("Timepoint $goto", duration = 1500)
                }
            },
            StageUICommand("load Stack 1",null){_,_ ->
                zenMicroscope.debugStack(this.crovWithoutHoles)
            },
            StageUICommand("load Stack 2",null){_,_ ->
                zenMicroscope.debugStack(this.sd3Fly)
            }
        ))
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            LocalZenScene().main()
        }
    }
}