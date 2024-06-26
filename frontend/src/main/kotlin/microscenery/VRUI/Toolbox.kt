package microscenery.VRUI

import graphics.scenery.Scene
import graphics.scenery.attribute.spatial.Spatial
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackedDeviceType
import graphics.scenery.controls.TrackerRole
import graphics.scenery.controls.behaviours.Action
import graphics.scenery.controls.behaviours.Switch
import graphics.scenery.volumes.Colormap
import microscenery.MicroscenerySettings
import microscenery.Settings
import microscenery.UI.UIModel
import microscenery.VRUI.fromScenery.VRFastSelectionWheel
import microscenery.VRUI.fromScenery.VRFastSelectionWheel.Companion.toActions
import microscenery.VRUI.fromScenery.WheelMenu
import microscenery.stageSpace.StageSpaceManager
import org.scijava.ui.behaviour.DragBehaviour

class Toolbox(
    val scene: Scene,
    hmd: OpenVRHMD,
    buttons: List<OpenVRHMD.OpenVRButton>,
    controllerSide: List<TrackerRole>,
    customMenu: WheelMenu? = null,
    stageSpaceManager: StageSpaceManager? = null,
    uiModel: UIModel,
    enabled: () -> Boolean = {true}
) {
    val pointTool = PointEntityTool()
    val lineTool = LineEntityTool()
    val croppingTool = CroppingTool(uiModel)
    val pathAblationTool = stageSpaceManager?.let { PathAblationTool(stageSpaceManager = it, hmd = hmd, uiModel = uiModel) }
    val pointCloudAblationTool = stageSpaceManager?.let { PointCloudAblationTool(stageSpaceManager = it, hmd = hmd, uiModel = uiModel) }
    val ablationInkMoveTool = stageSpaceManager?.let { AblationInkMoveTool(stageSpaceManager)}
    val bubblesTool = BubblesTool()

    init {
        pointTool.visible = false
        lineTool.visible = false
        croppingTool.visible = false
        pathAblationTool?.visible = false
        pointCloudAblationTool?.visible = false

        var defaultMenu: List<Pair<String, (Spatial) -> Unit>> = listOf()

        fun addIfEnabled(key: String, name: String, command: (Spatial) -> Unit) {
            if (MicroscenerySettings.get(key, false)) {
                defaultMenu = defaultMenu + (name to command)
            }
        }

        addIfEnabled(Settings.VRToolbox.SlicingEnabled, "slicing tool") { device ->
            stageSpaceManager?.sliceManager?.stacks?.firstOrNull()?.let {
                val vol = it.volume
                scene.addChild(croppingTool)
                croppingTool.spatial().position = device.worldPosition()
                croppingTool.activate(vol)
            }
        }
        addIfEnabled(Settings.VRToolbox.DrawPointsEnabled, "point") { device ->
            scene.addChild(pointTool)
            pointTool.visible = true
            pointTool.spatial().position = device.worldPosition()
        }
        addIfEnabled(Settings.VRToolbox.DrawLineEnabled, "line") { device ->
            scene.addChild(lineTool)
            lineTool.visible = true
            lineTool.spatial().position = device.worldPosition()
        }
        pathAblationTool?.let{
            addIfEnabled(Settings.VRToolbox.PathAblationEnabled, "path ablation") { device ->
                scene.addChild(pathAblationTool)
                pathAblationTool.visible = true
                pathAblationTool.spatial().position = device.worldPosition()
            }}
        pointCloudAblationTool?.let{
            addIfEnabled(Settings.VRToolbox.PointAblationEnabled, "point ablation") { device ->
                scene.addChild(pointCloudAblationTool)
                pointCloudAblationTool.visible = true
                pointCloudAblationTool.spatial().position = device.worldPosition()
            }}
        ablationInkMoveTool?.let{
            addIfEnabled(Settings.VRToolbox.AblationInkMoverEnabled, "mover"){ device ->
                scene.addChild(ablationInkMoveTool)
                ablationInkMoveTool.visible = true
                ablationInkMoveTool.spatial().position = device.worldPosition()
            }}


        addIfEnabled(Settings.VRToolbox.BubblesEnabled, "bubbles") { device ->
            scene.addChild(bubblesTool)
            bubblesTool.visible = true
            bubblesTool.spatial().position = device.worldPosition()
        }
        addIfEnabled(Settings.VRToolbox.OptionsEnabled, "options") {
            val m = WheelMenu(hmd, listOf(Switch("hull", true) { scene.find("hullbox")?.visible = it }), true)
            m.spatial().position = it.worldPosition()
            scene.addChild(m)
        }
        addIfEnabled(Settings.VRToolbox.ColorChooserEnabled, "color") {
            stageSpaceManager?.sliceManager?.stacks?.firstOrNull()?.let { stack ->
                val volume = stack.volume
                val m = WheelMenu(hmd, Colormap.list().map {
                    Action(it) { volume.colormap = Colormap.get(it) }
                }.toList(), true)

                m.spatial().position = it.worldPosition()
                scene.addChild(m)
            }
        }

        class EnableableDragBehaviorWrapper(val isEnabled: () -> Boolean, val wrapped: DragBehaviour): DragBehaviour{
            var startEnabled = false
            override fun init(x: Int, y: Int) {
                if (isEnabled()) {
                    startEnabled = true
                    wrapped.init(x,y)
                } else {
                    startEnabled = false
                }
            }

            override fun drag(x: Int, y: Int) {
                if (startEnabled) wrapped.drag(x,y)
            }

            override fun end(x: Int, y: Int) {
                if (startEnabled) wrapped.end(x,y)
            }

        }


        // custom set to allow disableling
        val actions: List<Pair<String, (Spatial) -> Unit>> = customMenu?.let {
            defaultMenu + ("scene ops" to {
                customMenu.spatial().position = it.worldPosition()
                scene.addChild(customMenu)
            })
        } ?: defaultMenu
        hmd.events.onDeviceConnect.add { _, device, _ ->
            if (device.type == TrackedDeviceType.Controller) {
                device.model?.let { controller ->
                    if (controllerSide.contains(device.role)) {
                        val vrToolSelector = EnableableDragBehaviorWrapper(
                            enabled,
                            VRFastSelectionWheel(
                                controller.children.first().spatialOrNull()
                                    ?: throw IllegalArgumentException("The target controller needs a spatial."),
                                scene,
                                hmd,
                                actions.toActions(
                                    controller.children.first().spatialOrNull() ?: throw IllegalArgumentException(
                                        "The target controller needs a spatial."
                                    )
                                )
                            )
                        )
                        buttons.forEach { button ->
                            val name = "ToolBoxWheelMenu:${hmd.trackingSystemName}:${device.role}:$button"
                            hmd.addBehaviour(name, vrToolSelector)
                            hmd.addKeyBinding(name, device.role, button)
                        }
                    }
                }
            }
        }
    }
}