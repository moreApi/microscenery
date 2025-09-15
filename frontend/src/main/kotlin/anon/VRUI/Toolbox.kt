package anon.VRUI

import graphics.scenery.Scene
import graphics.scenery.attribute.spatial.Spatial
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackedDeviceType
import graphics.scenery.controls.TrackerRole
import graphics.scenery.controls.behaviours.Action
import graphics.scenery.controls.behaviours.Switch
import graphics.scenery.volumes.Colormap
import anon.MicroscenerySettings
import anon.Settings
import anon.UI.UIModel
import anon.VRUI.fromScenery.VRFastSelectionWheel
import anon.VRUI.fromScenery.VRFastSelectionWheel.Companion.toActions
import anon.VRUI.fromScenery.WheelMenu
import anon.stageSpace.StageSpaceManager

class Toolbox(
    val scene: Scene,
    hmd: OpenVRHMD,
    buttons: List<OpenVRHMD.OpenVRButton>,
    customMenu: WheelMenu? = null,
    stageSpaceManager: StageSpaceManager? = null,
    uiModel: UIModel
) {
    val menuSide = TrackerRole.RightHand

    val croppingTool = CroppingTool(uiModel)
    val pathAblationTool =
        stageSpaceManager?.let { PathAblationTool(stageSpaceManager = it, hmd = hmd, uiModel = uiModel) }
    val pointCloudAblationTool =
        stageSpaceManager?.let { PointCloudAblationTool(stageSpaceManager = it, hmd = hmd, uiModel = uiModel) }
    val ablationInkMoveTool = stageSpaceManager?.let { AblationInkMoveTool(stageSpaceManager, uiModel) }
    val measureTool = stageSpaceManager?.let { MeasureTool(stageSpaceManager = it, hmd = hmd, uiModel = uiModel) }
    val bubblesTool = BubblesTool(uiModel)

    init {
        var defaultMenu: List<Pair<String, (Spatial) -> Unit>> = listOf()

        fun addIfEnabled(key: String, name: String, command: (Spatial) -> Unit) {
            if (MicroscenerySettings.get(key, false)) {
                defaultMenu = defaultMenu + (name to command)
            }
        }

        fun addIfEnabled(key: String, name: String, tool: VRHandTool) {
            addIfEnabled(key, name) {
                tool.activate(uiModel, menuSide)
            }
        }

        measureTool?.let {
            addIfEnabled(Settings.VRToolbox.MeasuringEnabled, "measure tool", it)
//            addIfEnabled("todo", "measure tool") { _ ->
//
//
//                uiModel.rightVRController?.let { device ->
//                    uiModel.putInHand(device.role, measureTool)
//                    measureTool.spatial{
//                        //rotation.premul(device.model?.spatialOrNull()?.worldRotation()?.invert())
//                        position = Vector3f()
//                    }
//                    device.model?.addChild(measureTool)
//                }
//
//
//                //uiModel.rightVRController?.model?.addChild(measureTool)
//                //uiModel.inRightHand = measureTool
//
//                //measureTool.spatial().position = device.worldPosition()
//            }
        }

        addIfEnabled(Settings.VRToolbox.CroppingEnabled, "slicing tool", croppingTool)

        pathAblationTool?.let { addIfEnabled(Settings.VRToolbox.PathAblationEnabled, "path ablation", it) }
        pointCloudAblationTool?.let {
            addIfEnabled(Settings.VRToolbox.PointAblationEnabled, "point ablation", it)
        }
        ablationInkMoveTool?.let { addIfEnabled(Settings.VRToolbox.AblationInkMoverEnabled, "mover", it) }

        addIfEnabled(Settings.VRToolbox.BubblesEnabled, "bubbles", bubblesTool)

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
                    if (menuSide == device.role) {
                        val vrToolSelector =
                            VRFastSelectionWheel(
                                controller.children.first().spatialOrNull()
                                    ?: throw IllegalArgumentException("The target controller needs a spatial."),
                                scene,
                                hmd,
                                actions.toActions(
                                    controller.children.first().spatialOrNull() ?: throw IllegalArgumentException(
                                        "The target controller needs a spatial."
                                    )
                                ),
                                noSelection = {
                                    uiModel.inHand(menuSide)?.deactivate(uiModel)
                                }
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