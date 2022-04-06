package microscenery.VRUI

import graphics.scenery.Scene
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackerRole
import graphics.scenery.controls.behaviours.VRSelectionWheel
import graphics.scenery.volumes.Volume

class Toolbox(val scene: Scene, hmd: OpenVRHMD,button: List<OpenVRHMD.OpenVRButton>,
              controllerSide: List<TrackerRole>) {
    val pointTool = PointEntityTool()
    val croppingTool = CroppingTool()
    val tfe = TransferFunction1DEditor()


    init {
        pointTool.visible = false
        croppingTool.visible = false
        tfe.visible = false


        VRSelectionWheel.createAndSet(scene,
            hmd,
            button,
            controllerSide,
            listOf(
                "slicing tool" to { device ->
                    scene.addChild(croppingTool)
                    croppingTool.spatial().position = device.worldPosition()
                    croppingTool.activate(
                        scene.findByClassname("Volume").firstOrNull() as? Volume
                            ?: throw IllegalStateException("did not found a volume for slicing plane")
                    )
                },
                "point marker" to { device ->
                    scene.addChild(pointTool)
                    pointTool.visible = true
                    pointTool.spatial().position = device.worldPosition()
                },
                "transfer function" to { device ->
                    scene.addChild(tfe)
                    tfe.spatial {
                        position = device.worldPosition()
                        // this breaks everything :(
                        //rotation = Quaternionf(hmd.getOrientation()).conjugate().normalize()
                    }
                    tfe.visible = true
                    (scene.findByClassname("Volume").firstOrNull() as? Volume
                        ?: throw IllegalStateException("did not found a volume for transfer function editor"))
                        .transferFunction = tfe.transferFunction
                }
            )
        )


    }
}