package microscenery.VRUI

import fromScenery.utils.extensions.minus
import fromScenery.utils.extensions.times
import graphics.scenery.Box
import graphics.scenery.RichNode
import graphics.scenery.Sphere
import graphics.scenery.attribute.spatial.HasSpatial
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.behaviours.*
import graphics.scenery.numerics.Random
import graphics.scenery.utils.extensions.plus
import org.joml.Vector3f

class BubblesTool() : Box(Vector3f(0.05f, 0.13f, 0.05f)) {
    val tipLength = 0.025f
    val tip = Box(Vector3f(0.015f, tipLength, 0.015f))
    val inkOutput = RichNode()

    var lastBubble = System.currentTimeMillis()

    init {
        tip.spatial().position = Vector3f(0f, this.sizes.y / 2 + tipLength / 2, 0f)
        addChild(tip)
        inkOutput.spatial().position.y = tipLength / 2
        tip.addChild(inkOutput)

        this.addAttribute(Touchable::class.java, Touchable())
        this.addAttribute(Grabable::class.java, Grabable(lockRotation = false))
        this.addAttribute(
            Pressable::class.java, PerButtonPressable(
                mapOf(
                    OpenVRHMD.OpenVRButton.Trigger to SimplePressable(
                        onPress = { _,_ ->
                            placePoint(inkOutput)
                        },
                        onHold = { _,_ ->
                             if (lastBubble + 50 < System.currentTimeMillis()){
                                 placePoint(inkOutput)
                                 lastBubble = System.currentTimeMillis()
                             }
                        },
                        onRelease = { _,_ ->
                        }
                    ),
                    CLOSE_BUTTON to SimplePressable(onPress = { _,_ ->
                        this.visible = false
                        parent?.removeChild(this)
                    })
                )
            )
        )

    }

    private fun placePoint(pos: HasSpatial) {

        val ink = Sphere(0.015f)
        ink.material().diffuse = Vector3f(1f,1f,1f)
        ink.material().metallic = 1.0f
        ink.material().roughness = 0.0f
        ink.spatial().position = pos.spatial().worldPosition(Vector3f())
        ink.spatial().scale *= Random.randomFromRange(1f,5f)
        this.getScene()?.addChild(ink)

        val born = System.currentTimeMillis()
        val dir = pos.spatial().worldPosition() - tip.spatial().worldPosition()
        val speed = dir * Random.randomFromRange(0.5f,5f)

        ink.update +={
            if (born + 5000 < System.currentTimeMillis()){
                ink.parent?.removeChild(ink)
            }
            ink.spatial{
                position += speed
            }

        }
    }
}