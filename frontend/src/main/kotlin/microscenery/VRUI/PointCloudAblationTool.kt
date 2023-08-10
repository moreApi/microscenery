package microscenery.VRUI

import fromScenery.utils.extensions.minus
import graphics.scenery.Box
import graphics.scenery.RichNode
import graphics.scenery.Sphere
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.behaviours.*
import graphics.scenery.utils.Wiggler
import microscenery.*
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector3f

class PointCloudAblationTool(
    var pointColor: Vector3f = Vector3f(1.0f, 0.5f, 0.0f),
    val stageSpaceManager: StageSpaceManager,
    hmd: OpenVRHMD,
) : Box(Vector3f(0.05f, 0.13f, 0.05f)) {
    private val tip: Box
    private val inkOutput: RichNode

    private var eraser = false

    private var preparedInk: Sphere? = null
    private var placedPoints: List<Sphere> = emptyList()

    init {
        MicroscenerySettings.setVector3fIfUnset("Ablation.PointCloud.MinDistUm",Vector3f(100f))

        val tipLength = 0.025f
        tip = Box(Vector3f(0.015f, tipLength, 0.015f))
        tip.spatial().position = Vector3f(0f, this.sizes.y / 2 + tipLength / 2, 0f)
        addChild(tip)
        inkOutput = RichNode()
        inkOutput.spatial().position.y = tipLength / 2
        tip.addChild(inkOutput)
        prepareInk()

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
                            if (timeOfLastInk + timeBetweenInks < System.currentTimeMillis()) {
                                placeInk()
                                timeOfLastInk = System.currentTimeMillis()
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
                                    Switch("eraser",false) { toggleEraser(it) },
                                    Action("plan ablation") { planAblation() },
                                    Action("remove plan") { removePlan() },
                                    Action("ablate") { ablate() },
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

    private fun planAblation() {
        TODO("Not yet implemented")
    }
    private fun removePlan() {
        TODO("Not yet implemented")
    }
    private fun ablate() {
        TODO("Not yet implemented")
    }

    private fun toggleEraser(it: Boolean) {

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
        val ink = Sphere( 0.005f)
        ink.material().diffuse = pointColor
        ink.material().metallic = 0.0f
        ink.material().roughness = 1.0f

        inkOutput.addChild(ink)
        preparedInk = ink
    }

    private fun placeInk() {
        val minDistance = MicroscenerySettings.getVector3("Ablation.PointCloud.MinDistUm") ?: return

        val ink = preparedInk ?: return
        val posInStageSpace = stageSpaceManager.worldToStageSpace(ink.spatial().worldPosition())
        val coerced = stageSpaceManager.hardware.hardwareDimensions().coercePosition(posInStageSpace, null, true)

        if (posInStageSpace != coerced) {
            val oldWiggler = ink.getAttributeOrNull(Wiggler::class.java)
            if (oldWiggler?.active == true) return
            //ink is out of stage space bounds, wiggle in protest
            ink.addAttribute(Wiggler::class.java,Wiggler(ink.spatial(), range = 0.01f, lifeTimeMillis = 300))
            return
        }
        if( placedPoints.any {
                (it.spatial().position -  ink.spatial().worldPosition()).absolute().isFullyLessThan(minDistance)}
        ){
            //todo does not work
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
}