package microscenery.primitives

import fromScenery.utils.extensions.minus
import graphics.scenery.Sphere
import graphics.scenery.attribute.material.Material
import graphics.scenery.attribute.spatial.HasSpatial
import graphics.scenery.controls.behaviours.Grabable
import graphics.scenery.controls.behaviours.Touchable
import graphics.scenery.primitives.Cylinder
import microscenery.UP
import microscenery.detach
import org.joml.Quaternionf
import org.joml.Vector3f


/**
 *
 * @param lineMaterial defaults to material of Node
 */
open class LineNode( connectedTo: List<LineNode> = emptyList(), radius: Float = 1f,var lineMaterial: Material? = null ) :
    Sphere(radius, segments = 16) {


    var lines = emptyList<LineConnection>()
        private set

    init {
        name = "LineNode"
        this.addAttribute(Touchable::class.java, Touchable())
        this.addAttribute(Grabable::class.java, Grabable(lockRotation = true))

        connectedTo.forEach { connectTo(it) }
    }

    fun connectTo(to: LineNode){
        if (to == this) return
        if (getConnectionTo(to) != null) return

        val connection = LineConnection(this, to, lineMaterial ?: this.material(), this.radius * .5f)
        this.addChild(connection)
        this.lines += connection
        to.lines += connection
    }

    fun removeConnection(to: LineNode){
        val connection = getConnectionTo(to) ?: return
        this.lines -= connection
        to.lines -= connection
        connection.detach()
    }

    private fun getConnectionTo(to: LineNode): LineConnection?{
        if (to == this) return null
        return lines.firstOrNull { it.from == to || it.to == to }
    }

    class LineConnection(val from: HasSpatial, val to: HasSpatial, material: Material, radius: Float)
        : Cylinder(radius = radius, 1f, 20){
        init {
            name = "LineConnection"
            setMaterial(material)
            update += {
                val diff = to.spatial().position - from.spatial().position
                spatial {
                    scale.y = diff.length() / (parent?.spatialOrNull()?.scale?.y ?: 1f)
                    position = Vector3f()
                    rotation = Quaternionf().rotationTo(UP, diff)
                }
            }
        }
    }
}