package microscenery.example.network

import graphics.scenery.volumes.TransferFunctionEditor
import microscenery.DefaultScene
import microscenery.UI.StageSpaceUI
import microscenery.lightSleepOnCondition
import microscenery.network.RemoteMicroscopeClient
import microscenery.signals.ServerState
import microscenery.stageSpace.MicroscopeLayout
import microscenery.stageSpace.StageSpaceManager
import org.zeromq.ZContext


class RemoteMicroscopeClientScene : DefaultScene() {
    val stageSpaceManager: StageSpaceManager

    init {
        val zContext = ZContext()
        val client = RemoteMicroscopeClient(zContext = zContext)
        stageSpaceManager = StageSpaceManager(
            client, scene, hub, addFocusFrame = true,
            //layout = MicroscopeLayout.Scape(MicroscopeLayout.Axis.X, 90.0),
            layout = MicroscopeLayout.Default(),
            scaleDownFactor = 300f
        )

        //stageSpaceManager.stageRoot.spatial().scale *= Vector3f(1f, 1f, 2f)

        lightSleepOnCondition { stageSpaceManager.hardware.status().state == ServerState.MANUAL }
        lightSleepOnCondition { stageSpaceManager.hardware.hardwareDimensions().imageSize.x != 0 }
        stageSpaceManager.focusTarget?.spatial()?.position = stageSpaceManager.focus.spatial().position
        //DemoBehavior(50f, stageSpaceManager).randomLive()

        TransferFunctionEditor(tfContainer = stageSpaceManager)
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

        inputHandler?.let {
            StageSpaceUI.stageUserInteraction(stageSpaceManager, it, cam)
        }

    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            RemoteMicroscopeClientScene().main()
        }
    }

}
