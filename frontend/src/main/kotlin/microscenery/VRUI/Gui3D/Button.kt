package microscenery.VRUI.Gui3D

import graphics.scenery.controls.behaviours.Pressable
import graphics.scenery.controls.behaviours.SimplePressable
import graphics.scenery.controls.behaviours.Touchable
import microscenery.changeColorWithTouchable
import org.joml.Vector3f

class Button(text: String, height: Float = 1f,command: () -> Unit): TextBox(text,height = height) {
    /** only visually */
    var pressed: Boolean = false
    set(value) {
        field = value
        if (value){
            this.spatial{
                scale.z = 0.5f
                position.z = -0.25f
                needsUpdate = true
            }
            box.changeColorWithTouchable(pressedColor)
        } else {
            this.spatial{
                scale.z = 1f
                position.z = 0f
                needsUpdate = true
            }
            box.changeColorWithTouchable(Vector3f(1f))
        }
    }

    var stayPressed = false

    var pressedColor = Vector3f(0.5f)

    init {
        box.addAttribute(Touchable::class.java, Touchable())
        box.addAttribute(Pressable::class.java, SimplePressable(
            onPress = {
                command()
                pressed = true
            },
            onRelease = {
                pressed = false || stayPressed
            }
        ))
    }
}
