package microscenery.VRUI

import graphics.scenery.Hub
import graphics.scenery.Node
import graphics.scenery.Scene
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.volumes.Volume
import microscenery.MicrosceneryHub
import microscenery.UI.UIModel
import microscenery.VRUI.Gui3D.Column
import microscenery.VRUI.Gui3D.Row
import microscenery.VRUI.Gui3D.TextBox
import microscenery.signals.MicroscopeStatus
import microscenery.signals.ServerState
import microscenery.stageSpace.StageSpaceManager
import org.joml.Quaternionf
import org.joml.Vector3f

/**
 * Label on top of the stage space.
 * Needs [StageSpaceManager] and [UIModel] to initialize
 */
class StageSpaceLabel(val scene: Scene, val msHub: MicrosceneryHub) {

    private val stageSpaceManager = msHub.getAttribute(StageSpaceManager::class.java)

    private val microscopeStatusLabel = TextBox("microscope status")


    init {
        val timepointLabel = TextBox("time point: 1/1")
        msHub.getAttribute(UIModel::class.java).changeEvents += { event ->
            when (event.kProperty) {
                UIModel::selected -> {
                    (event.new as? Node)?.let { node ->
                        when (node) {
                            is Volume -> {
                                timepointLabel.text = "Time Point: ${node.currentTimepoint + 1}/${node.timepointCount}"
                            }
                            else -> {
                                timepointLabel.text = "Time Point: no volume"
                            }
                        }
                    }
                }
            }
        }

        val column = Column(
            Row(timepointLabel),
            Row(microscopeStatusLabel)
        )

        column.spatial {
            scale = Vector3f(0.1f)

            // follow head/cam rotation
            fun updateMSLabelRotation(viewOrientation: Quaternionf) {
                rotation = Quaternionf(viewOrientation).conjugate().normalize()
            }
            val hmd = msHub.getAttribute(Hub::class.java).get<OpenVRHMD>()

            column.update += {
                if (hmd != null) {
                    updateMSLabelRotation(hmd.getOrientation())
                } else {
                    scene.activeObserver?.let { updateMSLabelRotation(it.spatial().rotation) }
                }

                // move it on top of the stage space
                val centerW = stageSpaceManager.stageRoot.spatial().worldPosition(stageSpaceManager.stageAreaCenter)
                centerW.y = stageSpaceManager.stageAreaBorders.generateBoundingBox()?.asWorld()?.max?.y ?: centerW.y
                centerW.y += 0.1f
                position = centerW
            }
        }
        scene.addChild(column)
        column.pack()

    }

    internal fun updateMicroscopeStatusLabel(signal: MicroscopeStatus) {
        microscopeStatusLabel.text = "Microscope: " + when(signal.state){
            ServerState.LIVE -> "sending data"
            ServerState.MANUAL -> "idle"
            ServerState.SHUTTING_DOWN -> "shutting down"
            ServerState.STACK -> "imaging"
            ServerState.STARTUP -> "startup"
            ServerState.ABLATION -> "ablating"
        }
        (microscopeStatusLabel.parent as? Row)?.pack()
    }
}