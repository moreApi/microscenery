package microscenery.VRUI

import fromScenery.utils.extensions.times
import graphics.scenery.Box
import graphics.scenery.RichNode
import graphics.scenery.Sphere
import graphics.scenery.controls.behaviours.Grabable
import graphics.scenery.controls.behaviours.Touchable
import org.joml.Vector3f

class ModifiableBox: RichNode() {

    init {
        // make the box look like it is scaling from one corner but keeps the grab center point in the middle
        val scalePivot = RichNode()
        scalePivot.spatial{
            position = Vector3f(-0.5f)
        }
        this.addChild(scalePivot)

        val box = Box()
        box.addAttribute(Grabable::class.java, Grabable(target = this, lockRotation = true))
        box.addAttribute(Touchable::class.java, Touchable())
        box.spatial{
            position = Vector3f(1f)
        }
        scalePivot.addChild(box)

        val scaleHandle = Sphere(0.1f)
        scaleHandle.addAttribute(Grabable::class.java,Grabable(lockRotation = true))
        scaleHandle.addAttribute(Touchable::class.java,Touchable())
        scaleHandle.spatial{
            position = Vector3f(1f)
        }
        scaleHandle.update += {
            scaleHandle.spatial{
                position = position.max(Vector3f(0.01f))
                scale = Vector3f(position.length())

                box.spatial().scale = position
                box.spatial().position = position
                scalePivot.spatial().position = position * -0.5f
            }
        }

        this.addChild(scaleHandle)
    }
}