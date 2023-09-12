package microscenery.example.microscope

import microscenery.DefaultScene
import microscenery.MicroscenerySettings
import microscenery.Settings
import microscenery.UI.StageSpaceUI
import microscenery.hardware.MicroscopeHardware
import microscenery.stageSpace.StageSpaceManager
import microscenery.zenSysConCon.ZenBlueTCPConnector
import microscenery.zenSysConCon.ZenMicroscope
import microscenery.zenSysConCon.sysCon.SysConNamedPipeConnector
import org.joml.Vector3f
import org.mockito.Mockito
import kotlin.concurrent.thread

class LocalZenScene : DefaultScene(withSwingUI = false) {
    val stageSpaceManager: StageSpaceManager

    init {
        MicroscenerySettings.set(Settings.StageSpace.HideFocusFrame,true)
        MicroscenerySettings.set(Settings.StageSpace.HideFocusTargetFrame,true)

        cam.spatial().position = Vector3f(0f, 0f, 5f)


//        val id = """C:\Nextcloud\Zeiss\Experiment-19.czi"""
//        val id = """C:\Nextcloud\Zeiss\sd3\20230712_488_square_ring.czi"""
        val id = """C:\Users\JanCasus\volumes\Zeiss\marina-sd3-drosophila1.czi"""

        val zenBlue: ZenBlueTCPConnector = Mockito.mock(ZenBlueTCPConnector::class.java)
        val sysCon: SysConNamedPipeConnector = Mockito.mock(SysConNamedPipeConnector::class.java)
        val zenMicroscope = ZenMicroscope(zenBlue, sysCon)

        val hardware: MicroscopeHardware = zenMicroscope
        stageSpaceManager = StageSpaceManager(hardware, scene, hub)


        thread {
            Thread.sleep(5000)
            logger.info("loading $id")
            zenMicroscope.debugStack(id)
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
        StageSpaceUI(stageSpaceManager).stageUI(this,inputHandler)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            LocalZenScene().main()
        }
    }
}