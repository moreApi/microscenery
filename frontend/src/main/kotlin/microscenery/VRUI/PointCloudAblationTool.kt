package microscenery.VRUI

import fromScenery.utils.extensions.minus
import graphics.scenery.Box
import graphics.scenery.RichNode
import graphics.scenery.Sphere
import graphics.scenery.attribute.spatial.HasSpatial
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.behaviours.*
import graphics.scenery.utils.Wiggler
import microscenery.*
import microscenery.primitives.Pyramid
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector3f

class PointCloudAblationTool(
    var pointColor: Vector3f = Vector3f(1.0f, 0.5f, 0.0f),
    val stageSpaceManager: StageSpaceManager,
    hmd: OpenVRHMD,
) : Box(Vector3f(0.04f, 0.10f, 0.04f)) {
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

    init {
        MicroscenerySettings.setVector3fIfUnset("Ablation.PointCloud.MinDistUm",Vector3f(100f))

        val tipLength = 0.06f
        tip = Pyramid(0.03f,0.03f,tipLength)
        tip.spatial().position = Vector3f(0f, sizes.y/2, 0f)
        addChild(tip)
        inkOutput = RichNode()
        inkOutput.spatial().position.y = tipLength
        tip.addChild(inkOutput)
        prepareInk()

        eraserHead.spatial{
            position = Vector3f(0f,tipLength,0f)
        }
        Touch("point ablation eraser", eraserHead,{placedPoints})

        this.addAttribute(Touchable::class.java, Touchable())
        this.addAttribute(Grabable::class.java, Grabable(lockRotation = false))

        var startOfPressMenu = System.currentTimeMillis()
        var timeOfLastInk = System.currentTimeMillis()
        var toolHid = false
        val timeForLongClickMillis = 1000
        val timeBetweenInks = 50
        this.addAttribute(
            Pressable::class.java, PerButtonPressable(
                mapOf(
                    OpenVRHMD.OpenVRButton.Trigger to SimplePressable(
                        onHold = {
                            if (!eraserActive){
                                if (timeOfLastInk + timeBetweenInks < System.currentTimeMillis()) {
                                    placeInk()
                                    timeOfLastInk = System.currentTimeMillis()
                                }
                            } else {
                                inkTouchingEraser.forEach { it.detach() }
                                inkTouchingEraser = emptyList()
                            }
                        }
                    ),
                    CLOSE_BUTTON to SimplePressable(
                        onPress = {
                            startOfPressMenu = System.currentTimeMillis()
                            toolHid = false
                        },
                        onHold = {
                            if (startOfPressMenu + timeForLongClickMillis < System.currentTimeMillis() && !toolHid) {
                                // hide tool
                                this.visible = false
                                parent?.removeChild(this)
                                toolHid = true
                            }
                        },
                        onRelease = {
                            if (toolHid) {
                                // tool has been hidden
                                return@SimplePressable
                            }
                            // open menu
                            val m = WheelMenu(
                                hmd, listOf(
                                    Action("clear all") { clearInk() },
                                    Switch("eraser",eraserActive) { eraserActive = !eraserActive }
                                ), true
                            )
                            m.spatial().position = it.worldPosition()
                            tip.getScene()?.addChild(m)
                        }
                    )
                )
            )
        )
    }

    private fun toggleEraser(eraserActive: Boolean) {
        if (eraserActive){
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
        val ink = Ink( 0.005f,pointColor,this)
        inkOutput.addChild(ink)
        preparedInk = ink
    }

    private fun placeInk() {
        val minDistance = MicroscenerySettings.getVector3("Ablation.PointCloud.MinDistUm") ?: return

        val ink = preparedInk ?: return
        val posInStageSpace = stageSpaceManager.worldToStageSpace(ink.spatial().worldPosition())
        val coerced = stageSpaceManager.hardware.hardwareDimensions().coercePosition(posInStageSpace, null, true)

        if (posInStageSpace != coerced) {
            //ink is out of stage space bounds, wiggle in protest
            Wiggler(ink,0.01f,300)
            return
        }
        if( placedPoints.any {
                (it.spatial().position -  ink.spatial().worldPosition()).absolute().isFullyLessThan(minDistance)}
        ){
            //todo thiss needs to be in stage coordinates
            return //point is to close to another one
        }

        ink.spatial().position = ink.spatial().worldPosition()

        stageSpaceManager.worldToStageSpace(ink.spatial())
        ink.parent?.removeChild(ink)
        stageSpaceManager.stageRoot.addChild(ink)
        placedPoints = placedPoints + ink

        preparedInk = null
        prepareInk()
    }

    class Ink(radius: Float, color: Vector3f, source: PointCloudAblationTool):Sphere(radius){
        init {
            this.addAttribute(Touchable::class.java, Touchable(
                onTouch = {source.inkTouchingEraser += this},
                onRelease = {source.inkTouchingEraser -= this}
            ))

            material().diffuse = color
            material().metallic = 0.0f
            material().roughness = 1.0f
        }
    }
}