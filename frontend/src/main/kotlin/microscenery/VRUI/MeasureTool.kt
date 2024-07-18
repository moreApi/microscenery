package microscenery.VRUI

import graphics.scenery.Box
import graphics.scenery.RichNode
import graphics.scenery.attribute.material.DefaultMaterial
import graphics.scenery.attribute.spatial.Spatial
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.behaviours.*
import microscenery.UI.UIModel
import microscenery.VRUI.Gui3D.Row
import microscenery.VRUI.Gui3D.TextBox
import microscenery.VRUI.fromScenery.VRFastSelectionWheel
import microscenery.detach
import microscenery.primitives.LineNode
import microscenery.stageSpace.StageSpaceManager
import microscenery.stageSpace.WorldToLocalPosSync
import org.joml.Vector3f


/**
 * VR measuring tool
 *
 * Behavior:
 *  never have a point prepared
 *  on trigger:
 *      place point when not touching one
 *      delete point if touching one
 *  use grab forwarding to move points -> place to points then move point for dynamic measurement
 */
class MeasureTool(
    var lineColor: Vector3f = Vector3f(1.0f, 0.5f, 0.0f),
    val stageSpaceManager: StageSpaceManager,
    val hmd: OpenVRHMD,
    uiModel: UIModel
) : Box(Vector3f(0.05f, 0.13f, 0.05f)), VRHandTool {
    private val tip: Box
    private val pointOutput: RichNode

    private var measurePoints: List<MeasurePoint> = emptyList()

    private var menu: VRFastSelectionWheel? = null

    init {
        val tipLength = 0.025f
        tip = Box(Vector3f(0.015f, tipLength, 0.015f))
        tip.spatial().position = Vector3f(0f, this.sizes.y / 2 + tipLength / 2, 0f)
        tip.material().diffuse = lineColor
        addChild(tip)
        pointOutput = RichNode()
        pointOutput.spatial().position.y = tipLength / 2
        tip.addChild(pointOutput)
        val touch = Touch("Measuring Tool touch", tip, { measurePoints })

        //this.initVRHandToolAndPressable(uiModel,
       initVRHandToolAndPressable(uiModel,PerButtonPressable(
            mapOf(
                OpenVRHMD.OpenVRButton.Trigger to SimplePressable(
                    onRelease = { _, _ ->
                        if (touch.selected.isNotEmpty()) {
                            deletePoint(touch.selected.first() as? MeasurePoint)
                        } else {
                            placePoint()
                        }
                    }
                ),
                OpenVRHMD.OpenVRButton.Menu to SimplePressable(
                    onPress = { controllerSpatial, _ ->
                        val scene = getScene() ?: return@SimplePressable
                        val m = VRFastSelectionWheel(
                            controllerSpatial, scene, hmd, listOf(
                                Action("clear all") { clearAll() },
                                Action("undo") { deleteLastPoint() },
                                Action("hide") {
                                    this.visible = false
                                    this.detach()
                                })
                        )

                        m.init(0, 0)
                        menu = m
                    },
                    onHold =
                    { _, _ ->
                        menu?.drag(0, 0)
                    },
                    onRelease =
                    { _, _ ->
                        menu?.end(0, 0)
                    }
                )
            )
        ))
    }


    override fun getTipCollider(): Spatial = tip.spatial()

    private fun placePoint() {
        val newPoint = MeasurePoint()
        newPoint.material().diffuse = lineColor
        val worldPos = pointOutput.spatial().worldPosition()
        newPoint.spatial().position = stageSpaceManager.worldToStageSpace(worldPos, true)
        stageSpaceManager.stageRoot.addChild(newPoint)

        measurePoints.lastOrNull()?.connectTo(newPoint)
        measurePoints += newPoint

        newPoint.lines.filter {
            it.getAttributeOrNull(MeasureLabel::class.java) == null
        }.forEach { newLine ->
            val lineMidpoint = RichNode()
            lineMidpoint.spatial().position = Vector3f(0f, 0.5f, 0f)
            newLine.addChild(lineMidpoint)

            val labelPivot = RichNode()
            WorldToLocalPosSync(lineMidpoint, labelPivot)
            labelPivot.spatial {
                position.y = 0.01f
                scale = Vector3f(0.05f)
            }
            newLine.getScene()?.addChild(labelPivot)

            val label = TextBox("x mu", thickness = 0.2f)
            newLine.update += {
                val l = newLine.spatial().scale.y
                label.text = "%.3f".format(l) + " mu"
            }
            labelPivot.addChild(Row(label))

            newLine.addAttribute(MeasureLabel::class.java, MeasureLabel())
        }
    }

    private fun deletePoint(point: MeasurePoint?) {
        val index = measurePoints.indexOf(point)
        if (index < 0) {
            return
        }
        val prev = measurePoints.getOrNull(index - 1)
        val next = measurePoints.getOrNull(index + 1)
        measurePoints -= point!!

        prev?.removeConnection(point)
        next?.removeConnection(point)
        next?.let { prev?.connectTo(it) }

        point.detach()
    }

    private fun deleteLastPoint() {
        val point = measurePoints.lastOrNull() ?: return
        measurePoints -= point
        measurePoints.lastOrNull()?.removeConnection(point)
        point.detach()
    }

    private fun clearAll() {
        measurePoints.forEach {
            it.detach()
        }
        measurePoints = emptyList()
    }

    class MeasurePoint : LineNode() {
        init {
            setMaterial(DefaultMaterial().apply { diffuse = Vector3f(0.7f) })
        }
    }

    /**
     * a placeholder to access the attribute functionality
     */
    class MeasureLabel
}
