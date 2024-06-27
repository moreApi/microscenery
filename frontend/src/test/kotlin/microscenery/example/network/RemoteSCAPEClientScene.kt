package microscenery.example.network

import microscenery.*
import microscenery.UI.ScapeViewerUI
import microscenery.UI.StageSpaceUI
import microscenery.UI.StageUICommand
import microscenery.network.RemoteMicroscopeClient
import microscenery.signals.ServerState
import microscenery.stageSpace.MicroscopeLayout
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector3f
import org.zeromq.ZContext


class RemoteSCAPEClientScene : DefaultScene(withSwingUI = true) {
    val stageSpaceManager: StageSpaceManager
    val msHub = MicrosceneryHub(hub)

    init {
        ScapeViewerUI.scapeViewerSettings()

        val zContext = ZContext()
        val client = RemoteMicroscopeClient(zContext = zContext)
        stageSpaceManager = StageSpaceManager(
            client, scene, msHub,
            layout = MicroscopeLayout.Scape(MicroscopeLayout.Axis.Z, -40.5),
            viewMode = true
        )


        lightSleepOnCondition { stageSpaceManager.hardware.status().state == ServerState.MANUAL }
        lightSleepOnCondition { stageSpaceManager.hardware.hardwareDimensions().imageSize.x != 0 }
        stageSpaceManager.focusTarget?.spatial()?.position = stageSpaceManager.focus.spatial().position

    }

    override fun inputSetup() {
        super.inputSetup()
        StageSpaceUI(stageSpaceManager).stageUI(this,inputHandler, msHub)

        val scapeUI = ScapeViewerUI(msHub)
        scapeUI.resetView()
        StageSpaceUI(stageSpaceManager).stageUI(this, inputHandler, msHub, listOf(
            StageUICommand("Right", "") { _, _ ->
                scapeUI.setViewDirection(Vector3f(-1f, 0f, 0f))
            },
            StageUICommand("Left", "") { _, _ ->
                scapeUI.setViewDirection(Vector3f(1f, 0f, 0f))
            }, StageUICommand("Top", "") { _, _ ->
                scapeUI.setViewDirection(Vector3f(0f, -1f, -0.1f))
            },
            StageUICommand("Bottom", "") { _, _ ->
                scapeUI.setViewDirection(Vector3f(0f, 1f, -0.1f))
            },
            StageUICommand("reset", "") { _, _ ->
                scapeUI.resetView()
            }
        ))
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            RemoteSCAPEClientScene().main()
        }
    }

}
