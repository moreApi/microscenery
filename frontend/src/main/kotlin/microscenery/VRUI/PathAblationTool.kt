package microscenery.VRUI

import fromScenery.utils.extensions.minus
import graphics.scenery.Box
import graphics.scenery.RichNode
import graphics.scenery.Sphere
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.behaviours.*
import graphics.scenery.primitives.Cylinder
import graphics.scenery.utils.Wiggler
import microscenery.UP
import microscenery.detach
import microscenery.stageSpace.StageSpaceManager
import org.joml.Quaternionf
import org.joml.Vector3f

/**
 *
 * Naming in this class:
 * ink = point = ablation point => ablation path node
 */
class PathAblationTool(
    var lineColor: Vector3f = Vector3f(1.0f, 0.5f, 0.0f),
    val stageSpaceManager: StageSpaceManager,
    hmd: OpenVRHMD,
) :
    Box(Vector3f(0.05f, 0.13f, 0.05f)) {
    private val tip: Box
    private val inkOutput: RichNode

    private var preparedInk: InkLine? = null
    private var lastInk: InkLine? = null

    init {
        val tipLength = 0.025f
        tip = Box(Vector3f(0.015f, tipLength, 0.015f))
        tip.spatial().position = Vector3f(0f, this.sizes.y / 2 + tipLength / 2, 0f)
        addChild(tip)
        inkOutput = RichNode()
        inkOutput.spatial().position.y = tipLength / 2
        tip.addChild(inkOutput)

        var startOfPress = System.currentTimeMillis()
        var menuOpened = false
        val timeForLongClickMillis = 1000

        this.addAttribute(Touchable::class.java, Touchable())
        this.addAttribute(Grabable::class.java, Grabable(lockRotation = false))
        this.addAttribute(
            Pressable::class.java, PerButtonPressable(
                mapOf(
                    OpenVRHMD.OpenVRButton.Trigger to SimplePressable(
                        onPress = {
                            startOfPress = System.currentTimeMillis()
                            menuOpened = false
                        },
                        onHold = {
                            if (startOfPress + timeForLongClickMillis < System.currentTimeMillis() && !menuOpened) {
                                val m = WheelMenu(
                                    hmd, listOf(
                                        Action("clear path") { clearInk() },
                                        Action("undo") { undoLastPoint() }
                                    ), true
                                )
                                m.spatial().position = it.worldPosition()
                                tip.getScene()?.addChild(m)
                                menuOpened = true
                            }
                        },
                        onRelease = {
                            if (menuOpened) {
                                // menu has be opened
                                return@SimplePressable
                            }
                            if (preparedInk == null) {
                                prepareInk()
                            } else {
                                placeInk()
                            }
                        }
                    ),
                    CLOSE_BUTTON to SimplePressable(onPress = {
                        this.visible = false
                        parent?.removeChild(this)
                    })
                )
            )
        )
        prepareInk()
    }

    /**
     * Stick a fresh sphere of ink to the tip
     */
    private fun prepareInk() {
        if (preparedInk != null) logger.warn("Someone has left some old ink and ordered new ink.")
        val ink = InkLine(lastInk, 0.015f)
        ink.material().diffuse = lineColor
        ink.material().metallic = 0.0f
        ink.material().roughness = 1.0f

        inkOutput.addChild(ink)
        preparedInk = ink
    }

    private fun placeInk() {
        val ink = preparedInk ?: return
        val posInStageSpace = stageSpaceManager.worldToStageSpace(ink.spatial().worldPosition())
        val coerced = stageSpaceManager.hardware.hardwareDimensions().coercePosition(posInStageSpace, null)

        if (posInStageSpace != coerced) {
            //ink is out of stage space bounds
            Wiggler(ink, range = 0.01f, lifeTimeMillis = 300)
            return
        }

        ink.spatial().position = ink.spatial().worldPosition()

        stageSpaceManager.worldToStageSpace(ink.spatial())
        ink.parent?.removeChild(ink)
        stageSpaceManager.stageRoot.addChild(ink)
        //getScene()?.addChild(ink)
        ink.addLine() //workaround

        lastInk = ink
        preparedInk = null
        prepareInk()
    }

    private fun clearInk() {
        if (lastInk == null) return
        var cur = lastInk
        while (cur != null) {
            cur.detach()
            cur = cur.previous
        }
        lastInk = null

        preparedInk?.detach()
        preparedInk = null
        prepareInk()

    }

    private fun undoLastPoint() {
        val tmp = lastInk ?: return
        lastInk = tmp.previous
        tmp.detach()
        preparedInk?.detach()
        prepareInk()
    }

    internal class InkLine(val previous: InkLine? = null, radius: Float = 0.015f) :
        Sphere(radius, segments = 16) {
        val line: Cylinder = Cylinder(0.005f, 1f, 20)

        init {
            this.addAttribute(Touchable::class.java, Touchable())
            this.addAttribute(Grabable::class.java, Grabable(lockRotation = true))
        }

        /**
         * adding a visible = false line from the beginning does not work for some reason. This is a workaround.
         */
        fun addLine() {
            if (previous == null) return

            line.material().diffuse = this.material().diffuse
            line.material().metallic = 0.0f
            line.material().roughness = 1.0f
            this.addChild(line)

            update += {
                let {
                    val diff =
                        (previous.spatial().position - this.spatial().position).div(spatial().scale) //position is in stage space, everything below ablation point not
                    line.spatial {
                        scale.y = diff.length()
                        rotation = Quaternionf().rotationTo(UP, diff)
                    }
                }
            }
        }
    }
}
