package microscenery.simulation

import graphics.scenery.Node
import graphics.scenery.attribute.material.Material
import org.joml.Vector3f

/**
 * Attribute
 */
interface Simulatable {
    fun intensity(pos: Vector3f): Short
    companion object{
        fun Node.showMaterial(){
            ifMaterial { cullingMode = Material.CullingMode.None }
        }

        fun Node.hideMaterial(){
            ifMaterial { cullingMode = Material.CullingMode.FrontAndBack }
        }
    }
}

