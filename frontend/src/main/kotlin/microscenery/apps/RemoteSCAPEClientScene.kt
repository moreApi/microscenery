package microscenery.apps

import microscenery.*
import microscenery.UI.ScapeViewerUI
import microscenery.UI.StageSpaceUI
import microscenery.network.RemoteMicroscopeClient
import microscenery.signals.ServerState
import microscenery.stageSpace.MicroscopeLayout
import microscenery.stageSpace.StageSpaceManager
import org.zeromq.ZContext


class RemoteSCAPEClientScene : DefaultScene(withSwingUI = true) {
    val stageSpaceManager: StageSpaceManager
    val msHub = MicrosceneryHub(hub)

    init {
        ScapeViewerUI.scapeViewerSettings()

        MicroscenerySettings.setIfUnset(Settings.MMMicroscope.AngleOfScapeSheet,-40.5)

        val zContext = ZContext()
        val client = RemoteMicroscopeClient(zContext = zContext)
        stageSpaceManager = StageSpaceManager(
            client, scene, msHub,
            layout = MicroscopeLayout.Scape(
                MicroscopeLayout.Axis.Z,
                MicroscenerySettings.get(Settings.MMMicroscope.AngleOfScapeSheet,-40.5)),
            viewMode = true
        )

        lightSleepOnCondition { stageSpaceManager.hardware.status().state == ServerState.MANUAL }
        lightSleepOnCondition { stageSpaceManager.hardware.hardwareDimensions().imageSize.x != 0 }

    }

    override fun inputSetup() {
        super.inputSetup()

        val scapeUI = ScapeViewerUI(msHub)
        scapeUI.resetView()
        StageSpaceUI(stageSpaceManager).stageUI(this, inputHandler, msHub, scapeUI.orthoViewCommands())
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            RemoteSCAPEClientScene().main()
        }
    }

}
