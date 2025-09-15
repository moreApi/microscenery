package anon.simulation

import fromScenery.utils.extensions.times
import graphics.scenery.attribute.spatial.HasSpatial
import graphics.scenery.attribute.spatial.Spatial
import anon.copy
import org.joml.Matrix4f
import org.joml.Vector3f

/**
 * Attribute
 */
interface Simulatable {
    fun intensity(pos: Vector3f): Short

    /**
     * Get world matrix as if stage root is the scene root.
     */
    fun HasSpatial.inverseStageMatrix(stageRoot: Spatial): Matrix4f {
        val target = stageRoot.world.copy().invertAffine() * Matrix4f(spatial().world)
        return target.invertAffine()
    }
}

