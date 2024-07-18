package microscenery.VRUI

import fromScenery.utils.extensions.minus
import graphics.scenery.Box
import graphics.scenery.RichNode
import graphics.scenery.Sphere
import graphics.scenery.attribute.spatial.HasSpatial
import graphics.scenery.attribute.spatial.Spatial
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.behaviours.*
import graphics.scenery.utils.Wiggler
import microscenery.*
import microscenery.UI.UIModel
import microscenery.VRUI.fromScenery.VRFastSelectionWheel
import microscenery.primitives.Pyramid
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector3f

class PointCloudAblationTool(
    var pointColor: Vector3f = Vector3f(1.0f, 0.5f, 0.0f),
    val stageSpaceManager: StageSpaceManager,
    hmd: OpenVRHMD,
    uiModel: UIModel
) : Box(Vector3f(0.04f, 0.10f, 0.04f)), VRHandTool {
    private val tip: HasSpatial
    private val inkOutput: RichNode
    private var eraserHead: Box = Box(Vector3f(0.02f))

    private var eraserActive = false
        set(value) {
            toggleEraser(value)
            field = value
        }

    private var preparedInk: Ink? = null
    private var placedPoints: List<Sphere> = emptyList()
    private var inkTouchingEraser: List<Ink> = emptyList()

    private var menu: VRFastSelectionWheel? = null

    override fun getTipCollider(): Spatial = inkOutput.spatial()

    init {
        MicroscenerySettings.setVector3fIfUnset(Settings.Ablation.PointTool.MinDistUm, Vector3f(0.1f))

        material().diffuse = Vector3f(1f)

        val tipLength = 0.06f
        tip = Pyramid(0.03f, 0.03f, tipLength)
        tip.spatial().position = Vector3f(0f, sizes.y / 2, 0f)
        tip.material().diffuse = Vector3f(1f)
        addChild(tip)
        inkOutput = RichNode()
        inkOutput.spatial().position.y = tipLength
        tip.addChild(inkOutput)
        prepareInk()
        update += {
            preparedInk?.spatial()?.scale = stageSpaceManager.stageRoot.spatial().worldScale()
        }

        eraserHead.spatial {
            position = Vector3f(0f, tipLength, 0f)
        }
        Touch("point ablation eraser", eraserHead, { placedPoints })

        this.addAttribute(Touchable::class.java, Touchable())

        var timeOfLastInk = System.currentTimeMillis()
        val timeBetweenInks = 50

        this.initVRHandToolAndPressable(uiModel, PerButtonPressable(
            mapOf(
                OpenVRHMD.OpenVRButton.Trigger to SimplePressable(
                    onHold = { _, _ ->
                        if (!eraserActive) {
                            if (timeOfLastInk + timeBetweenInks < System.currentTimeMillis()) {
                                placeInk()
                                timeOfLastInk = System.currentTimeMillis()
                            }
                        } else {
                            inkTouchingEraser.forEach { it.detach() }
                            inkTouchingEraser = emptyList()
                        }
                    }
                )
            ).plus(TOOL_MENU_BUTTON.map {
                it to SimplePressable(
                    onPress = { controllerSpatial, _ ->
                        val scene = getScene() ?: return@SimplePressable
                        val m = VRFastSelectionWheel(
                            controllerSpatial, scene, hmd, listOf(
                                Action("clear all") { clearInk() },
                                Switch("eraser", eraserActive) { eraserActive = !eraserActive },
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
            })
        )
        )
    }

    private fun toggleEraser(eraserActive: Boolean) {
        if (eraserActive) {
            inkTouchingEraser = emptyList()
            tip.addChild(eraserHead)
            eraserHead.spatial().needsUpdate = true
        } else {
            eraserHead.detach()
        }
    }

    private fun clearInk() {
        placedPoints.forEach {
            it.detach()
        }
        placedPoints = emptyList()
    }

    /**
     * Stick a fresh sphere of ink to the tip
     */
    private fun prepareInk() {
        if (preparedInk != null) logger.warn("Someone has left some old ink and ordered new ink.")
        val ink = Ink(MicroscenerySettings.get(Settings.Ablation.SizeUM, 8f) * 0.5f, pointColor, this)
        ink.spatial().scale = stageSpaceManager.stageRoot.spatial().worldScale()
        inkOutput.addChild(ink)
        preparedInk = ink
    }

    private fun placeInk() {
        val minDistance = MicroscenerySettings.getVector3(Settings.Ablation.PrecisionUM) ?: Vector3f(1f)

        val ink = preparedInk ?: return
        val posInStageSpace = stageSpaceManager.worldToStageSpace(ink.spatial().worldPosition())
        val coerced = stageSpaceManager.hardware.hardwareDimensions().coercePosition(posInStageSpace, null, true)

        if (posInStageSpace != coerced) {
            //ink is out of stage space bounds, wiggle in protest
            Wiggler(ink, 0.01f, 300)
            return
        }
        if (placedPoints.any { placedInk ->
                (placedInk.spatial().position - posInStageSpace).absolute().isFullyLessThan(minDistance)
            }
        ) {
            return //point is to close to another one
        }
        // we are sure we will place this ink. Set preparedInk to null so the update function won't interfere.
        preparedInk = null

        ink.getAttributeOrNull(Wiggler::class.java)?.deativate()
            ?.join() // avoids a bug where a point is moved after placing

        ink.spatial().position = ink.spatial().worldPosition()
        stageSpaceManager.worldToStageSpace(ink.spatial())
        ink.spatial().scale = Vector3f(1f)

        ink.parent?.removeChild(ink)
        stageSpaceManager.stageRoot.addChild(ink)
        placedPoints = placedPoints + ink

        prepareInk()
    }

    class Ink(radius: Float, color: Vector3f, source: PointCloudAblationTool) : Sphere(radius) {
        init {
            this.addAttribute(Touchable::class.java, Touchable(
                onTouch = { source.inkTouchingEraser += this },
                onRelease = { source.inkTouchingEraser -= this }
            ))

            material().diffuse = color
            material().metallic = 0.0f
            material().roughness = 1.0f
        }
    }
}