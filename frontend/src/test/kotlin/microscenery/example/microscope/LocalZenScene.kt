package microscenery.example.microscope

import microscenery.DefaultScene
import microscenery.MicroscenerySettings
import microscenery.Settings
import microscenery.UI.StageSpaceUI
import microscenery.UI.StageUICommand
import microscenery.hardware.MicroscopeHardware
import microscenery.showMessage2
import microscenery.stageSpace.StageSpaceManager
import microscenery.zenSysConCon.ZenBlueTCPConnector
import microscenery.zenSysConCon.ZenMicroscope
import microscenery.zenSysConCon.sysCon.SysConConnection
import org.joml.Vector3f
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import kotlin.concurrent.thread

class LocalZenScene : DefaultScene(withSwingUI = true) {
    val stageSpaceManager: StageSpaceManager

    val zenMicroscope: ZenMicroscope
    val crovWithoutHoles = """C:\Users\JanCasus\volumes\Zeiss\20230915_488_corvoria_without_holes.czi"""
    val crovHoles = """C:\Users\JanCasus\volumes\Zeiss\20230915_488_corvoria_with_holes.czi"""

    init {
        MicroscenerySettings.set(Settings.StageSpace.HideFocusFrame,true)
        MicroscenerySettings.set(Settings.StageSpace.HideFocusTargetFrame,true)

        cam.spatial().position = Vector3f(0f, 0f, 5f)

//        val id = """C:\Users\JanCasus\Zeiss\Experiment-19.czi"""
        val id = """C:\Nextcloud\Zeiss\sd3\20230712_488_square_ring.czi"""
        val id2 = """C:\Nextcloud\Zeiss\sd3\20230712_488_square_ring2.czi"""
//        val id = """C:\Nextcloud\Zeiss\sd3\20230712_488_square_ring3.czi"""
        //val id = """C:\Nextcloud\Zeiss\marina-sd3-drosophila2.czi"""


        val zenBlue: ZenBlueTCPConnector = Mockito.mock(ZenBlueTCPConnector::class.java)
        val sysCon: SysConConnection = Mockito.mock(SysConConnection::class.java)
        whenever(zenBlue.getCurrentDocument()).thenReturn(id)
        zenMicroscope = ZenMicroscope(zenBlue, sysCon)

        val hardware: MicroscopeHardware = zenMicroscope
        stageSpaceManager = StageSpaceManager(hardware, scene, hub)

        thread {
            Thread.sleep(100)
            val l1 = crovWithoutHoles
            logger.info("init $l1")
            //zenMicroscope.debugStack(l1)

            Thread.sleep(3000)
            logger.info("init $id2")
//            zenMicroscope.debugStack(id2)
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
        StageSpaceUI(stageSpaceManager).stageUI(this,inputHandler, listOf(
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
                zenMicroscope.debugStack(this.crovHoles)
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