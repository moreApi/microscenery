package microscenery.example.network

import microscenery.DefaultVRScene
import microscenery.UI.StageSpaceUI
import microscenery.VRUI.VRUIManager
import microscenery.lightSleepOnCondition
import microscenery.network.RemoteMicroscopeClient
import microscenery.signals.ServerState
import microscenery.stageSpace.StageSpaceManager
import org.zeromq.ZContext


class RemoteMicroscopeClientSceneVR : DefaultVRScene() {
    val stageSpaceManager: StageSpaceManager

    init {
        val zContext = ZContext()
        val client = RemoteMicroscopeClient(zContext = zContext)
        stageSpaceManager = StageSpaceManager(
            client, scene, hub, addFocusFrame = true,
        )

        lightSleepOnCondition { stageSpaceManager.hardware.status().state == ServerState.MANUAL }
        lightSleepOnCondition { stageSpaceManager.hardware.hardwareDimensions().imageSize.x != 0 }
        stageSpaceManager.focusTarget?.spatial()?.position = stageSpaceManager.focus.spatial().position

    }

    override fun inputSetup() {
        super.inputSetup()
        val ssui = StageSpaceUI(stageSpaceManager)
        inputHandler?.let { ssui.stageKeyUI(it,this.cam) }

        VRUIManager.initBehavior(
            scene, hmd, inputHandler,
            stageSpaceUI = ssui
        ) {
            stageSpaceManager.stageRoot
        }

    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            RemoteMicroscopeClientSceneVR().main()
        }
    }

}
