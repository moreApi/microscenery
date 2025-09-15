package anon.scenes.network

import anon.*
import anon.UI.StageSpaceUI
import anon.VRUI.VRUIManager
import anon.network.RemoteMicroscopeClient
import anon.signals.ServerState
import anon.stageSpace.StageSpaceManager
import org.joml.Vector3f
import org.zeromq.ZContext


class RemoteMicroscopeClientSceneVR : DefaultVRScene() {
    val stageSpaceManager: StageSpaceManager
    lateinit var  msHub: MicrosceneryHub

    init {
        MicroscenerySettings.set(Settings.StageSpace.HideFocusFrame,true)
        MicroscenerySettings.set(Settings.StageSpace.HideFocusTargetFrame,true)
        MicroscenerySettings.set(Settings.Ablation.Enabled, true)
        MicroscenerySettings.set(Settings.VRToolbox.PointAblationEnabled, true)
        MicroscenerySettings.set(Settings.VRToolbox.PathAblationEnabled, true)
        MicroscenerySettings.set(Settings.VRToolbox.AblationInkMoverEnabled, true)
        MicroscenerySettings.set(Settings.VRToolbox.CroppingEnabled, true)
        MicroscenerySettings.setVector3f(Settings.Ablation.PointTool.MinDistUm, Vector3f(2.5f))
        MicroscenerySettings.set(Settings.StageSpace.ColorMap, "plasma")
        MicroscenerySettings.set(Settings.UI.ShowSelectionIndicator, false)
        MicroscenerySettings.set(Settings.UI.FlySpeed, 0.1f)

        val zContext = ZContext()
        val client = RemoteMicroscopeClient(zContext = zContext)
        msHub = MicrosceneryHub(hub)
        stageSpaceManager = StageSpaceManager(client, scene, msHub)

        lightSleepOnCondition { stageSpaceManager.hardware.status().state == ServerState.MANUAL }
        lightSleepOnCondition { stageSpaceManager.hardware.hardwareDimensions().imageSize.x != 0 }

    }

    override fun inputSetup() {
        super.inputSetup()
        val ssui = StageSpaceUI(stageSpaceManager)
        inputHandler?.let { ssui.stageKeyUI(it,this.cam) }

        VRUIManager.initBehavior(
            scene, hmd, inputHandler,
            stageSpaceUI = ssui,
            msHub = msHub
        )

    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            RemoteMicroscopeClientSceneVR().main()
        }
    }

}
