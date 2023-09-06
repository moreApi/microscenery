package microscenery.VRUI.Gui3D

import graphics.scenery.Box
import graphics.scenery.RichNode
import graphics.scenery.controls.behaviours.Pressable
import graphics.scenery.controls.behaviours.SimplePressable
import graphics.scenery.controls.behaviours.Touchable
import org.joml.Vector3f

class Switch(label: String, value:Boolean, middleAlign:Boolean = false,  onChange: (Boolean) -> Unit)
    : Row(margin = 0.2f, middleAlign = middleAlign) {
    init {
        name = "Switch $label"

        this.addChild(Knob(value,onChange = onChange))
        this.addChild(TextBox(label))
    }

    private class Knob(var value:Boolean,
                       val onColor: Vector3f = Vector3f(0f,0.8f,0f),
                       val offColor: Vector3f = Vector3f(0.5f),
                       val onChange: (Boolean) -> Unit)
        : RichNode("Knob"), Gui3DElement{

        //background
        val bg = Box(Vector3f(2f,1f,0.3f))

        override val width: Float
            get() = bg.sizes.x
        override val height: Float
            get() = bg.sizes.y

        init {
            bg.spatial {
                position = Vector3f(bg.sizes.x*0.5f, bg.sizes.y*0.5f, -bg.sizes.z*0.5f)
            }
            bg.material().diffuse = Vector3f(1f)
            this.addChild(bg)
            val knob = Box(Vector3f(bg.sizes.x * 0.4f))
            bg.addChild(knob)

            knob.spatial().position.x = bg.sizes.x * 0.25f * if (value) 1 else -1
            knob.material().diffuse = if (value) onColor else offColor

            knob.addAttribute(Pressable::class.java, SimplePressable(onRelease = {
                val newColor = if (toggle()) onColor else offColor
                val touch = knob.getAttribute(Touchable::class.java)
                if (touch.originalDiffuse != null){
                    // this might screw with [VRTouch]s coloring, but it's not too bad as the menu is rebuild
                    // for every opening anew
                    touch.originalDiffuse = newColor
                } else {
                    knob.material().diffuse = newColor
                }

                knob.spatial().position.x = bg.sizes.x * 0.25f * if (value) 1 else -1
                knob.spatial().needsUpdate = true
            }))
            // make it go red on touch
            knob.addAttribute(Touchable::class.java, Touchable())
        }

        fun toggle(): Boolean{
            value = !value
            onChange(value)
            return value
        }
    }

}