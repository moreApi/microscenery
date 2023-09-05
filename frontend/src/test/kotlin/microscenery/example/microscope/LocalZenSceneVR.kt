package microscenery.example.microscope

import microscenery.DefaultVRScene
import microscenery.MicroscenerySettings
import microscenery.Settings
import microscenery.UI.StageSpaceUI
import microscenery.VRUI.VRUIManager
import microscenery.hardware.MicroscopeHardware
import microscenery.stageSpace.StageSpaceManager
import microscenery.zenSysConCon.ZenBlueTCPConnector
import microscenery.zenSysConCon.ZenMicroscope
import microscenery.zenSysConCon.sysCon.SysConNamedPipeConnector
import org.joml.Vector3f
import org.mockito.Mockito
import kotlin.concurrent.thread

class LocalZenSceneVR : DefaultVRScene() {
    lateinit var  stageSpaceManager: StageSpaceManager

    override fun init() {
        super.init()
        cam.spatial().position = Vector3f(0f, 0f, 5f)

        MicroscenerySettings.set(Settings.Ablation.Enabled, true)
        MicroscenerySettings.set(Settings.VRToolbox.PointAblationEnabled, true)
        MicroscenerySettings.set(Settings.VRToolbox.PathAblationEnabled, true)
        MicroscenerySettings.set(Settings.VRToolbox.SlicingEnabled, true)


//        val id = """C:\Users\JanCasus\Zeiss\Experiment-19.czi"""
        //val id = """C:\Nextcloud\Zeiss\sd3\20230712_488_square_ring.czi"""
        //val id = """C:\Nextcloud\Zeiss\sd3\20230712_488_square_ring3.czi"""
        val id = """C:\Nextcloud\Zeiss\marina-sd3-drosophila2.czi"""

        val zenBlue: ZenBlueTCPConnector = Mockito.mock(ZenBlueTCPConnector::class.java)
        val sysCon: SysConNamedPipeConnector = Mockito.mock(SysConNamedPipeConnector::class.java)
        val zenMicroscope = ZenMicroscope(zenBlue, sysCon)

        val hardware: MicroscopeHardware = zenMicroscope
        stageSpaceManager = StageSpaceManager(hardware, scene, hub, addFocusFrame = false)
        stageSpaceManager.focus.visible = false

        thread {
            Thread.sleep(2000)

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
        val ssUI = StageSpaceUI(stageSpaceManager)

        inputHandler?.let {
            ssUI.stageKeyUI(it, cam)
        }

        VRUIManager.initBehavior(
            scene, hmd, inputHandler,
            stageSpaceUI = ssUI
        ) {
            stageSpaceManager.stageRoot
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            LocalZenSceneVR().main()
        }
    }
}