package microscenery.simulation

import org.joml.Vector3f

/**
 * Attribute
 */
interface Simulatable {
    fun intensity(pos: Vector3f): Short

}

