package microscenery.VRUI

import graphics.scenery.Box
import graphics.scenery.Sphere
import graphics.scenery.controls.behaviours.SimplePressable
import org.joml.Vector3f

class PointEntityTool: Box(Vector3f(0.05f, 0.13f, 0.05f)) {
    init {
        val tip = Box(Vector3f(0.025f, 0.025f, 0.025f))
        tip.spatial {
            position = Vector3f(0f, 0.08f, 0f)
        }
        addChild(tip)

        this.addVRToolFunctionality ( pressable = SimplePressable(
            onPress = {
                val ink = Sphere(0.03f)
                ink.spatial().position=tip.spatial().worldPosition()
                this.parent?.addChild(ink)
            }
        )
        )
    }
}