package microscenery.VRUI

import graphics.scenery.Box
import graphics.scenery.Sphere
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.behaviours.*
import org.joml.Vector3f

class PointEntityTool : Box(Vector3f(0.05f, 0.13f, 0.05f)) {
    init {
        val tip = Box(Vector3f(0.025f, 0.025f, 0.025f))
        tip.spatial {
            position = Vector3f(0f, 0.08f, 0f)
        }
        addChild(tip)

        this.addAttribute(Touchable::class.java, Touchable())
        this.addAttribute(Grabable::class.java, Grabable(lockRotation = false))
        this.addAttribute(
            Pressable::class.java, PerButtonPressable(
                mapOf(
                    OpenVRHMD.OpenVRButton.Trigger to SimplePressable(
                        onPress = {
                            val ink = Sphere(0.03f)
                            ink.spatial().position = tip.spatial().worldPosition(Vector3f())
                            this.getScene()?.addChild(ink)
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
}