package microscenery.simulation

import graphics.scenery.attribute.spatial.HasSpatial
import org.joml.Matrix4f
import org.joml.Vector3f

/**
 * Attribute
 */
interface Simulatable {
    fun intensity(pos: Vector3f): Short


    fun HasSpatial.inverseStageMatrix(): Matrix4f {
        val target = Matrix4f(this.spatial().model)

        var currentNode = this
        while (currentNode.parent?.name != "stage root"){
            currentNode = currentNode.parent as? HasSpatial ?: break
            currentNode.spatial().model.mulAffine(target,target)
        }

        return target.invertAffine()
    }
}

