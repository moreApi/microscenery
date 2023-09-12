package microscenery.example.network

import microscenery.*
import microscenery.UI.StageSpaceUI
import microscenery.VRUI.VRUIManager
import microscenery.network.RemoteMicroscopeClient
import microscenery.signals.ServerState
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector3f
import org.zeromq.ZContext


class RemoteMicroscopeClientSceneVR : DefaultVRScene() {
    val stageSpaceManager: StageSpaceManager

    init {
        MicroscenerySettings.set(Settings.StageSpace.HideFocusFrame,true)
        MicroscenerySettings.set(Settings.StageSpace.HideFocusTargetFrame,true)
        MicroscenerySettings.set(Settings.Ablation.Enabled, true)
        MicroscenerySettings.set(Settings.VRToolbox.PointAblationEnabled, true)
        MicroscenerySettings.set(Settings.VRToolbox.PathAblationEnabled, true)
        MicroscenerySettings.set(Settings.VRToolbox.SlicingEnabled, true)
        MicroscenerySettings.setVector3f(Settings.Ablation.PointTool.MinDistUm, Vector3f(5f))

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
