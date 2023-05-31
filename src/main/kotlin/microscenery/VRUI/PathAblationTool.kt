package microscenery.VRUI

import fromScenery.utils.extensions.minus
import graphics.scenery.Box
import graphics.scenery.RichNode
import graphics.scenery.Sphere
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.behaviours.*
import graphics.scenery.primitives.Cylinder
import microscenery.*
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

    private var preparedInk: AblationPoint? = null
    private var lastInk: AblationPoint? = null

    init {
        val tipLength = 0.025f
        tip = Box(Vector3f(0.015f, tipLength, 0.015f))
        tip.spatial().position = Vector3f(0f, this.sizes.y / 2 + tipLength / 2, 0f)
        addChild(tip)
        inkOutput = RichNode()
        inkOutput.spatial().position.y = tipLength / 2
        tip.addChild(inkOutput)

        var startOfPress = System.currentTimeMillis()
        val timeForLongClickMillis = 2000

        this.addAttribute(Touchable::class.java, Touchable())
        this.addAttribute(Grabable::class.java, Grabable(lockRotation = false))
        this.addAttribute(
            Pressable::class.java, PerButtonPressable(
                mapOf(
                    OpenVRHMD.OpenVRButton.Trigger to SimplePressable(
                        onPress = {
                            startOfPress = System.currentTimeMillis()
                        },
                        onHold = {
                            if (startOfPress + timeForLongClickMillis > System.currentTimeMillis()){
                                val m = WheelMenu(hmd, listOf(
                                    Action("clear path"){clearPath() },
                                    Action("undo"){undoLastPoint() },
                                    Action("pause"){pauseDrawing() },
                                    Action("ablate path"){ablatePath() },
                                ),true)
                                m.spatial().position = it.worldPosition()
                                tip.getScene()?.addChild(m)
                            }
                        },
                        onRelease = {
                            if (startOfPress + timeForLongClickMillis > System.currentTimeMillis()){
                                // menu has be opened
                                return@SimplePressable
                            }
                            if (preparedInk == null){
                                generateInk()
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
        generateInk()
    }

    private fun placeInk() {
        val ink = preparedInk ?: return
        stageSpaceManager.worldToStageSpace(ink.spatial())
        ink.parent?.removeChild(ink)
        stageSpaceManager.stageRoot.addChild(ink)

        preparedInk = null
        lastInk = ink
        generateInk()
    }

    /**
     * Stick a fresh sphere of ink to the tip
     */
    private fun generateInk(){
        if (preparedInk != null) logger.warn("Someone has left some old ink and ordered new ink.")
        val ink = AblationPoint(lastInk,0.015f)
        ink.material().diffuse = lineColor
        ink.material().metallic = 0.0f
        ink.material().roughness = 1.0f


        inkOutput.addChild(ink)
        preparedInk = ink
    }

    private fun clearPath(){
        if (lastInk == null) return
        var cur = lastInk
        while (cur != null){
            cur.detach()
            cur = cur.previous
        }
        lastInk = null

        preparedInk?.detach()
        preparedInk = null
        generateInk()

    }

    private fun undoLastPoint(){
        if (lastInk == null) return
        preparedInk?.detach()

        lastInk?.let {
            it.detach()
            inkOutput.addChild(it)
        }
    }

    private fun pauseDrawing(){
        if (preparedInk == null){
            generateInk()
        } else {
            preparedInk?.detach()
            preparedInk = null
        }
    }

    private fun ablatePath(){
        if (lastInk == null){
            logger.warn("No path to ablate found.")
            return
        }

        val path = mutableListOf<Vector3f>()
        val precision = MicroscenerySettings.getVector3("Ablation.precision") ?: Vector3f(1f)

        var cur = lastInk
        while (cur != null){
            path += stageSpaceManager.worldToStageSpace(cur.spatial().position)
            if (cur.previous != null){
                // sample line between last and current position
                (sampleLineSmooth(cur.previous!!.spatial().position, cur.spatial().position, precision))
                    .forEach {
                        path += it
                    }
            }
            cur = cur.previous
        }

        executeAblationCommandSequence(
            stageSpaceManager.hardware,
            buildLaserPath(path)
        )
    }
}

internal class AblationPoint(val previous: AblationPoint? = null, radius: Float = 0.015f): Sphere(radius){
    val line: Cylinder = Cylinder(0.005f, 1f, 20)

    init {
        line.material().diffuse = this.material().diffuse
         line.material().metallic = 0.0f
         line.material().roughness = 1.0f
         line.visible = false
         this.addChild(line)

        this.addAttribute(Touchable::class.java, Touchable())
        this.addAttribute(Grabable::class.java, Grabable(lockRotation = true))

         update += {
             previous?.let {
                 line.visible = true
                 val parent = this
                 line.spatial {
                     val diff = parent.spatial().position - previous.spatial().position
                     scale.y = diff.length()
                     rotation = Quaternionf().rotationTo(UP, diff)
                 }
             } ?: run{line.visible = false}
         }
     }
}