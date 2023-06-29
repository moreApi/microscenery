package microscenery.VRUI

import graphics.scenery.Node
import graphics.scenery.Scene
import graphics.scenery.attribute.spatial.Spatial
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackerRole
import graphics.scenery.controls.behaviours.Action
import graphics.scenery.controls.behaviours.Switch
import graphics.scenery.controls.behaviours.VRSelectionWheel
import graphics.scenery.controls.behaviours.WheelMenu
import graphics.scenery.volumes.Colormap
import graphics.scenery.volumes.Volume
import microscenery.stageSpace.StageSpaceManager

class Toolbox(
    val scene: Scene,
    hmd: OpenVRHMD,
    button: List<OpenVRHMD.OpenVRButton>,
    controllerSide: List<TrackerRole>,
    customMenu: WheelMenu? = null,
    stageSpaceManager: StageSpaceManager? = null,
    target: () -> Node?
) {
    val pointTool = PointEntityTool()
    val lineTool = LineEntityTool()
    val croppingTool = CroppingTool()
    val pathAblationTool = stageSpaceManager?.let { PathAblationTool( stageSpaceManager = it,hmd=hmd) }
    val bubblesTool = BubblesTool()

    init {
        pointTool.visible = false
        lineTool.visible = false
        croppingTool.visible = false
        pathAblationTool?.visible = false

        val defaultMenu: List<Pair<String, (Spatial) -> Unit>> = listOf("slicing tool" to { device ->
            target()?.let {
                if (it !is Volume) return@let
                scene.addChild(croppingTool)
                croppingTool.spatial().position = device.worldPosition()
                croppingTool.activate(it)
            }
        }, "point" to { device ->
            scene.addChild(pointTool)
            pointTool.visible = true
            pointTool.spatial().position = device.worldPosition()
        },"line" to { device ->
            scene.addChild(lineTool)
            lineTool.visible = true
            lineTool.spatial().position = device.worldPosition()
        },"ablation" to { device ->
            if (pathAblationTool != null){
                scene.addChild(pathAblationTool)
                pathAblationTool.visible = true
                pathAblationTool.spatial().position = device.worldPosition()
            } else {
                scene.addChild(bubblesTool)
                bubblesTool.visible = true
                bubblesTool.spatial().position = device.worldPosition()
            }
        }, "options" to {
            val m = WheelMenu(hmd, listOf(Switch("hull", true) { scene.find("hullbox")?.visible = it }), true)
            m.spatial().position = it.worldPosition()
            scene.addChild(m)
        }, "color" to {
            target()?.let { volume ->
                if (volume !is Volume) return@let
                val m = WheelMenu(hmd, Colormap.list().map {
                    Action(it) { volume.colormap = Colormap.get(it) }
                }.toList(), true)

                m.spatial().position = it.worldPosition()
                scene.addChild(m)
            }
        })

        VRSelectionWheel.createAndSet(
            scene, hmd, button, controllerSide,
            customMenu?.let {
                defaultMenu + ("scene ops" to {
                    customMenu.spatial().position = it.worldPosition()
                    scene.addChild(customMenu)
                })
            } ?: defaultMenu
        )
    }
}