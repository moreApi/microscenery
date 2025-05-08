package microscenery.scenes.network

import microscenery.*
import microscenery.UI.StageSpaceUI
import microscenery.network.RemoteMicroscopeClient
import microscenery.signals.ServerState
import microscenery.stageSpace.StageSpaceManager
import org.zeromq.ZContext


class RemoteMicroscopeClientScene(nonMicroscopeMode: Boolean = true) : DefaultScene(withSwingUI = true) {
    val stageSpaceManager: StageSpaceManager
    val msHub = MicrosceneryHub(hub)

    init {
        MicroscenerySettings.set(Settings.StageSpace.HideFocusFrame,false)
        MicroscenerySettings.set(Settings.StageSpace.HideFocusTargetFrame,false)
        MicroscenerySettings.set(Settings.MMMicroscope.IsMicromanagerMicroscope,false)

        val zContext = ZContext()
        val client = RemoteMicroscopeClient(zContext = zContext, nonMicroscopeMode = nonMicroscopeMode)
        stageSpaceManager = StageSpaceManager(
            client, scene, msHub,
            //viewMode = true
        )

        //stageSpaceManager.stageRoot.spatial().scale *= Vector3f(1f, 1f, 2f)

        lightSleepOnCondition { stageSpaceManager.hardware.status().state == ServerState.MANUAL }
        lightSleepOnCondition { stageSpaceManager.hardware.hardwareDimensions().imageSize.x != 0 }
        //DemoBehavior(50f, stageSpaceManager).randomLive()

        //TransferFunctionEditor(tfContainer = stageSpaceManager)
        //SettingsEditor(settings = MicroscenerySettings)

        /*
        thread {
            while (true) {
                Thread.sleep(200)
                scene
                @Suppress("UNUSED_EXPRESSION")
                client
                if (shouldBeHere != stageSpaceManager.stageRoot.children.size)
                    println("$shouldBeHere, ${stageSpaceManager.stageRoot.children.size}")
            }
        }
           */

    }

    override fun inputSetup() {
        super.inputSetup()
        StageSpaceUI(stageSpaceManager).stageUI(this,inputHandler, msHub)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            RemoteMicroscopeClientScene().main()
        }
    }

}
