package microscenery.VRUI

import fromScenery.utils.extensions.minus
import fromScenery.utils.extensions.plus
import graphics.scenery.Sphere
import graphics.scenery.attribute.spatial.HasSpatial
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.behaviours.*
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector3f

class AblationInkMoveTool(
    val stageSpaceManager: StageSpaceManager,
) : Sphere(0.025f,16) {

    private val defaultColor: Vector3f

    init {
        spatial {
            scale.y = 2f
            needsUpdate = true
        }

        material().diffuse = Vector3f(1f)
        defaultColor = material().diffuse

        this.addAttribute(Touchable::class.java, Touchable())
        this.addAttribute(Grabable::class.java, Grabable(lockRotation = false))

        var prevPosition = Vector3f(0f)
        var inks: List<HasSpatial> = emptyList()
        // min and max are in stage space
        var min = Vector3f()
        var max = Vector3f()
        this.addAttribute(
            Pressable::class.java, PerButtonPressable(
                mapOf(
                    OpenVRHMD.OpenVRButton.Trigger to SimplePressable(
                        onPress = { _,_ ->
                            prevPosition = this.spatial().worldPosition()
                            inks = stageSpaceManager.stageRoot.children.filter { it is PointCloudAblationTool.Ink || it is PathAblationTool.InkLine }.mapNotNull{ it as? HasSpatial }
                            min = inks.fold(Vector3f(Float.MAX_VALUE)){vec, pos ->
                                vec.min(pos.spatial().position)
                            }
                            max = inks.fold(Vector3f(Float.MIN_VALUE)){vec, pos ->
                                vec.max(pos.spatial().position)
                            }
                            material().diffuse = Vector3f(0f,1f,0.1f)

                        },
                        onHold = { _,_ ->
                            val div =  this.spatial().worldPosition() - prevPosition
                            val divInStageSpace = stageSpaceManager.worldToStageSpace(div,false)

                            if (inks.isEmpty()
                                || stageSpaceManager.hardware.hardwareDimensions().coercePosition(min+divInStageSpace, null, true) != min+divInStageSpace
                                || stageSpaceManager.hardware.hardwareDimensions().coercePosition(max+divInStageSpace, null, true) != max+divInStageSpace){
                                return@SimplePressable
                            }

                            inks.forEach {
                                it.ifSpatial {
                                    position += divInStageSpace
                                    needsUpdate = true
                                }
                            }
                            min += divInStageSpace
                            max += divInStageSpace
                            prevPosition = this.spatial().worldPosition()
                        },
                        onRelease = { _,_ ->
                            material().diffuse = defaultColor
                        }
                    )
                )
            )
        )
    }
}