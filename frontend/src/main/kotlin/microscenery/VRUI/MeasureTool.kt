package microscenery.VRUI

import graphics.scenery.Box
import graphics.scenery.RichNode
import graphics.scenery.attribute.material.DefaultMaterial
import graphics.scenery.attribute.spatial.Spatial
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.behaviours.*
import microscenery.UI.UIModel
import microscenery.VRUI.fromScenery.VRFastSelectionWheel
import microscenery.detach
import microscenery.primitives.LineNode
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector3f


/**
 * V1:
 * If no measure point has been placed prepare one on the tip.
 * If only one is placed prepare the next one on the tip and connect it to the first one.
 * If a connection is present show its length in microscope coordinates.
 * If two points are placed don't automatically prepare a new point at the tip.
 *      If a point gets dragged (either by hand or tool tip) move the dot.
 *      If empty space is clicked prepare a new point and connect it to the last point.
 * Pressing secondary button delets the point at tip
 *  and if none is present the one the tool is pointed at
 *  and if none is present the menu is opened
 *
 *  V2:
 *  never have a point prepared
 *  on trigger:
 *      place point when not touching one
 *      delete point if touching one
 *  use grab forwarding to move points -> place to points then move point for dynamic measurement
 */
class MeasureTool(
    var lineColor: Vector3f = Vector3f(1.0f, 0.5f, 0.0f),
    val stageSpaceManager: StageSpaceManager,
    hmd: OpenVRHMD,
    uiModel: UIModel
) : Box(Vector3f(0.05f, 0.13f, 0.05f)), VRHandTool {
    private val tip: Box
    private val pointOutput: RichNode

    private var measurePoints: List<MeasurePoint> = emptyList()

    private var menu: VRFastSelectionWheel? = null

    init {

        material().diffuse = Vector3f(1f)

        val tipLength = 0.025f
        tip = Box(Vector3f(0.015f, tipLength, 0.015f))
        tip.spatial().position = Vector3f(0f, this.sizes.y / 2 + tipLength / 2, 0f)
        tip.material().diffuse = Vector3f(1f)
        addChild(tip)
        pointOutput = RichNode()
        pointOutput.spatial().position.y = tipLength / 2
        tip.addChild(pointOutput)
        val touch = Touch("Measuring Tool touch", tip, {measurePoints})

        this.initVRHandToolAndPressable(uiModel, PerButtonPressable(
            mapOf(
                OpenVRHMD.OpenVRButton.Trigger to SimplePressable(
                    onRelease = { _, _ ->
                        if (touch.selected.isNotEmpty()){
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
        val worldPos = pointOutput.spatial().worldPosition()
        newPoint.spatial().position = stageSpaceManager.worldToStageSpace(worldPos,true)
        stageSpaceManager.stageRoot.addChild(newPoint)

        measurePoints.lastOrNull()?.connectTo(newPoint)
        measurePoints += newPoint
    }

    private fun deletePoint(point: MeasurePoint?) {
        val index = measurePoints.indexOf(point)
        if (index < 0) {return}
        val prev = measurePoints.getOrNull(index -1)
        val next = measurePoints.getOrNull(index +1)
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

    private fun clearAll(){
        measurePoints.forEach {
            it.detach()
        }
        measurePoints = emptyList()
    }

    class MeasurePoint : LineNode(){
        init {
            setMaterial(DefaultMaterial().apply { diffuse = Vector3f(0.7f) })
        }
    }
}
