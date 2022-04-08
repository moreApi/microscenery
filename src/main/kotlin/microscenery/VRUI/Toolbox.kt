package microscenery.VRUI

import graphics.scenery.Scene
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackerRole
import graphics.scenery.controls.behaviours.Switch
import graphics.scenery.controls.behaviours.VRSelectionWheel
import graphics.scenery.controls.behaviours.WheelMenu
import graphics.scenery.volumes.Volume
import microscenery.behaviors.VRScaleNode

class Toolbox(
    val scene: Scene,
    hmd: OpenVRHMD,
    button: List<OpenVRHMD.OpenVRButton>,
    controllerSide: List<TrackerRole>,
    volume: Volume
) {
    val pointTool = PointEntityTool()
    val croppingTool = CroppingTool()
    val tfe = TransferFunction1DEditor()

    init {
        pointTool.visible = false
        croppingTool.visible = false
        tfe.visible = false

        VRSelectionWheel.createAndSet(scene, hmd, button, controllerSide, listOf("slicing tool" to { device ->
            scene.addChild(croppingTool)
            croppingTool.spatial().position = device.worldPosition()
            croppingTool.activate(volume)
        }, "point marker" to { device ->
            scene.addChild(pointTool)
            pointTool.visible = true
            pointTool.spatial().position = device.worldPosition()
        }, "transfer function" to { device ->
            scene.addChild(tfe)
            tfe.spatial {
                position = device.worldPosition()
                // this breaks everything :(
                //rotation = Quaternionf(hmd.getOrientation()).conjugate().normalize()
            }
            tfe.visible = true
            volume.transferFunction = tfe.transferFunction
        }, "options" to {
            val m = WheelMenu(hmd, listOf(Switch("hull", true) { scene.find("hullbox")?.visible = it }), true)
            m.spatial().position = it.worldPosition()
            scene.addChild(m)
        }))

        VRScaleNode(volume)
    }
}