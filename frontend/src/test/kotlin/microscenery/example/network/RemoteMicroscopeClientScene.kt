package microscenery.example.network

import microscenery.DefaultScene
import microscenery.MicrosceneryHub
import microscenery.UI.StageSpaceUI
import microscenery.lightSleepOnCondition
import microscenery.network.RemoteMicroscopeClient
import microscenery.signals.ServerState
import microscenery.stageSpace.StageSpaceManager
import org.zeromq.ZContext


class RemoteMicroscopeClientScene : DefaultScene(withSwingUI = true) {
    val stageSpaceManager: StageSpaceManager

    init {
        val zContext = ZContext()
        val client = RemoteMicroscopeClient(zContext = zContext)
        val msHub = MicrosceneryHub(hub)
        stageSpaceManager = StageSpaceManager(
            client, scene, msHub,
            //layout = MicroscopeLayout.Scape(MicroscopeLayout.Axis.Z, -0.5f)
            //layout = MicroscopeLayout.Scape(MicroscopeLayout.Axis.X, 90.0),
        )

        //stageSpaceManager.stageRoot.spatial().scale *= Vector3f(1f, 1f, 2f)

        lightSleepOnCondition { stageSpaceManager.hardware.status().state == ServerState.MANUAL }
        lightSleepOnCondition { stageSpaceManager.hardware.hardwareDimensions().imageSize.x != 0 }
        stageSpaceManager.focusTarget?.spatial()?.position = stageSpaceManager.focus.spatial().position
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
        StageSpaceUI(stageSpaceManager).stageUI(this,inputHandler)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            RemoteMicroscopeClientScene().main()
        }
    }

}
