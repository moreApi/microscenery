package anon.VRUI

import fromScenery.utils.extensions.minus
import fromScenery.utils.extensions.times
import graphics.scenery.Box
import graphics.scenery.RichNode
import graphics.scenery.Sphere
import graphics.scenery.attribute.spatial.HasSpatial
import graphics.scenery.attribute.spatial.Spatial
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.behaviours.PerButtonPressable
import graphics.scenery.controls.behaviours.SimplePressable
import graphics.scenery.numerics.Random
import graphics.scenery.utils.extensions.plus
import anon.UI.UIModel
import org.joml.Vector3f

class BubblesTool(uiModel: UIModel) : Box(Vector3f(0.05f, 0.13f, 0.05f)), VRHandTool {
    val tipLength = 0.025f
    val tip = Box(Vector3f(0.015f, tipLength, 0.015f))
    val inkOutput = RichNode()

    override fun getTipCollider(): Spatial = tip.spatial()

    var lastBubble = System.currentTimeMillis()

    init {
        tip.spatial().position = Vector3f(0f, this.sizes.y / 2 + tipLength / 2, 0f)
        addChild(tip)
        inkOutput.spatial().position.y = tipLength / 2
        tip.addChild(inkOutput)

        initVRHandToolAndPressable(uiModel, PerButtonPressable(
            mapOf(
                OpenVRHMD.OpenVRButton.Trigger to SimplePressable(
                    onPress = { _, _ ->
                        placePoint(inkOutput)
                    },
                    onHold = { _, _ ->
                        if (lastBubble + 50 < System.currentTimeMillis()) {
                            placePoint(inkOutput)
                            lastBubble = System.currentTimeMillis()
                        }
                    },
                    onRelease = { _, _ ->
                    }
                )
            )
        )
        )
    }

    private fun placePoint(pos: HasSpatial) {

        val ink = Sphere(0.015f)
        ink.material().diffuse = Random.random3DVectorFromRange(0f, 1f)
        ink.material().metallic = Random.randomFromRange(0f, 1f)
        ink.material().roughness = Random.randomFromRange(0f, 1f)
        ink.spatial().position = pos.spatial().worldPosition(Vector3f())
        ink.spatial().scale *= Random.randomFromRange(1f, 5f)
        this.getScene()?.addChild(ink)

        val born = System.currentTimeMillis()
        val dir = pos.spatial().worldPosition() - tip.spatial().worldPosition()
        val speed = dir * Random.randomFromRange(0.5f, 5f)

        ink.update += {
            if (born + 5000 < System.currentTimeMillis()) {
                ink.parent?.removeChild(ink)
            }
            ink.spatial {
                position += speed
            }

        }
    }
}