package microscenery.VRUI.Gui3D

import graphics.scenery.controls.behaviours.Pressable
import graphics.scenery.controls.behaviours.SimplePressable
import graphics.scenery.controls.behaviours.Touchable

class Button(text: String, function: () -> Unit): TextBox(text) {
    init {
        box.addAttribute(Touchable::class.java, Touchable())
        box.addAttribute(Pressable::class.java, SimplePressable(
            onPress = {
                function()
                box.spatial{
                    scale.z *= 0.5f
                    needsUpdate = true
                }
            },
            onRelease = {
                box.spatial{
                    scale.z *= 2f
                    needsUpdate = true
                }
            }
        ))
    }
}
