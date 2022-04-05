package microscenery.VRUI

import graphics.scenery.Box
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.behaviours.*
import graphics.scenery.volumes.SlicingPlane
import graphics.scenery.volumes.Volume
import org.joml.Vector3f

class CroppingTool() : Box(Vector3f(0.2f, 0.01f, 0.2f)){
    var volume: Volume? = null
    val croppingPlane = SlicingPlane()

    init{
        this.addAttribute(Touchable::class.java, Touchable())
        this.addAttribute(Grabable::class.java, Grabable())
        this.addAttribute(Pressable::class.java,PerButtonPressable(mapOf(
            OpenVRHMD.OpenVRButton.Trigger to SimplePressable(onPress = {
                volume?.let { volume ->
                    val current = volume.slicingMode.id
                    // toggle through slicing modes, skipping none-mode
                    val next = (current % (Volume.SlicingMode.values().size - 1)) + 1
                    volume.slicingMode = Volume.SlicingMode.values()[next]
                }
            }),
            CLOSE_BUTTON to SimplePressable(onPress = {
                volume?.let { deactivate(it) }
            })
        )))

        this.addChild(croppingPlane)
    }

    fun activate(volume: Volume){
        this.visible = true
        this.volume = volume
        croppingPlane.addTargetVolume(volume)
        if (volume.slicingMode == Volume.SlicingMode.None)
            volume.slicingMode = Volume.SlicingMode.Cropping
    }

    fun deactivate(volume: Volume){
        this.visible = false
        this.volume = null
        croppingPlane.removeTargetVolume(volume)
        if (volume.slicingPlaneEquations.isEmpty()){
            volume.slicingMode = Volume.SlicingMode.None
        }
        parent?.removeChild(this)
    }




}