package microscenery.VRUI

import graphics.scenery.Box
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.behaviours.PerButtonPressable
import graphics.scenery.controls.behaviours.SimplePressable
import graphics.scenery.volumes.SlicingPlane
import graphics.scenery.volumes.Volume
import microscenery.UI.UIModel
import org.joml.Vector3f

class CroppingTool(uiModel: UIModel) : Box(Vector3f(0.2f, 0.02f, 0.2f)), VRHandTool {
    var volume: Volume? = null
    val croppingPlane = SlicingPlane()

    init {
        material().diffuse = Vector3f(1f)

        this.initVRHandToolAndPressable(
            uiModel, PerButtonPressable(
                mapOf(
                    OpenVRHMD.OpenVRButton.Trigger to SimplePressable(onPress = { _, _ ->
                        volume?.let { volume ->
                            val current = volume.slicingMode.id
                            // toggle through slicing modes, skipping none-mode
                            val next = (current % (Volume.SlicingMode.values().size - 1)) + 1
                            volume.slicingMode = Volume.SlicingMode.values()[next]
                        }
                    })
                )
            )
        )

        this.addChild(croppingPlane)
    }

    fun activate(volume: Volume) {
        this.visible = true
        this.volume = volume
        croppingPlane.addTargetVolume(volume)
        if (volume.slicingMode == Volume.SlicingMode.None)
            volume.slicingMode = Volume.SlicingMode.Cropping
    }

    fun deactivate(volume: Volume) {
        this.visible = false
        this.volume = null
        croppingPlane.removeTargetVolume(volume)
        if (volume.slicingPlaneEquations.isEmpty()) {
            volume.slicingMode = Volume.SlicingMode.None
        }
        parent?.removeChild(this)
    }


}