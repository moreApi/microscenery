package microscenery.VRUI

import UP
import graphics.scenery.Box
import graphics.scenery.RichNode
import graphics.scenery.Sphere
import graphics.scenery.attribute.spatial.HasSpatial
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.behaviours.*
import graphics.scenery.primitives.Cylinder
import graphics.scenery.utils.extensions.minus
import microscenery.DefaultScene
import org.joml.Quaternionf
import org.joml.Vector3f

class LineEntityTool(var lineColor: Vector3f = Vector3f(1.0f, 0.5f, 0.0f), var drawFrequency: Long = 500L) : Box(Vector3f(0.05f, 0.13f, 0.05f)) {

    private var lastPoint: Sphere? = null
    private var lastInkTimepoint = 0L

    init {
        val tipLength = 0.025f
        val tip = Box(Vector3f(0.015f,tipLength, 0.015f))
        tip.spatial().position = Vector3f(0f, this.sizes.y/2 + tipLength/2, 0f)
        addChild(tip)
        val inkOutput = RichNode()
        inkOutput.spatial().position.y = tipLength/2
        tip.addChild(inkOutput)

        this.addAttribute(Touchable::class.java, Touchable())
        this.addAttribute(Grabable::class.java, Grabable(lockRotation = false))
        this.addAttribute(
            Pressable::class.java, PerButtonPressable(
                mapOf(
                    OpenVRHMD.OpenVRButton.Trigger to SimplePressable(
                        onPress = {
                            placePoint(inkOutput)
                            lastInkTimepoint = System.currentTimeMillis()
                        },
                        onHold = {
                            if (System.currentTimeMillis() > lastInkTimepoint + drawFrequency){
                                placePoint(inkOutput)
                                lastInkTimepoint = System.currentTimeMillis()
                            }
                        },
                        onRelease = {
                            lastInkTimepoint = 0
                            lastPoint = null
                        }
                    ),
                    CLOSE_BUTTON to SimplePressable(onPress = {
                        this.visible = false
                        parent?.removeChild(this)
                    })
                )
            )
        )

    }

    private fun placePoint(tip: HasSpatial) {
        val ink = Sphere(0.015f)
        ink.material().diffuse = lineColor
        ink.material().metallic = 0.0f
        ink.material().roughness = 1.0f
        ink.spatial().position = tip.spatial().worldPosition(Vector3f())
        this.getScene()?.addChild(ink)
        drawLine(ink)
    }

    private fun drawLine(newPoint: Sphere){
        if (lastPoint == null){
            lastPoint = newPoint
            return
        }

        val diff = newPoint.spatial().worldPosition() - lastPoint!!.spatial().worldPosition()

        val line = Cylinder(0.005f, 1f, 20)
        line.material().diffuse = lineColor
        line.material().metallic = 0.0f
        line.material().roughness = 1.0f
        line.spatial {
            scale.y = diff.length()
            position = lastPoint!!.spatial().worldPosition()
            rotation = Quaternionf().rotationTo(UP,diff)
        }
        this.getScene()?.addChild(line)

        lastPoint = newPoint

    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            val base = DefaultScene( { scene, _ ->
                val b1 = Sphere(0.03f)
                b1.spatial().position = Vector3f(1f,01f,01f)
                scene.addChild(b1)
                val b2 = Sphere(0.03f)
                b2.spatial().position = Vector3f(-1f,-01f,-01f)
                scene.addChild(b2)

                val tool = LineEntityTool()
                scene.addChild(tool)

                tool.drawLine(b1)
                tool.drawLine(b2)

            })
            base.main()
        }
    }
}