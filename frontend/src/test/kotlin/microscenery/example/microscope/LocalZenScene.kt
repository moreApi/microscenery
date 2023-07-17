package microscenery.example.microscope

import microscenery.DefaultScene
import microscenery.UI.StageSpaceUI
import microscenery.hardware.MicroscopeHardware
import microscenery.stageSpace.StageSpaceManager
import microscenery.zenSysConCon.CZIFileWrapper
import microscenery.zenSysConCon.ZenMicroscope
import org.joml.Vector3f
import kotlin.concurrent.thread

class LocalZenScene : DefaultScene(withSwingUI = true) {
    val stageSpaceManager: StageSpaceManager

    init {
        cam.spatial().position = Vector3f(0f, 0f, 5f)


        //val id = """C:\Users\JanCasus\Zeiss\Experiment-19.czi"""
        val id = """C:\Users\JanCasus\Zeiss\sd3\20230712_488_square_ring.czi"""

        val cziWrap = CZIFileWrapper(id)
        val zenMicroscope = ZenMicroscope()

        val hardware: MicroscopeHardware = zenMicroscope
        stageSpaceManager = StageSpaceManager(hardware, scene, hub)

        thread {
            Thread.sleep(2000)

            zenMicroscope.stack(cziWrap)
        }
        thread {
            while (true){
                Thread.sleep(200)
                print(stageSpaceManager)
                print(zenMicroscope)
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