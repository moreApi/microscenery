package microscenery.primitives

import graphics.scenery.*
import graphics.scenery.attribute.material.Material
import graphics.scenery.net.Networkable
import graphics.scenery.utils.extensions.*
import org.joml.Vector2f
import org.joml.Vector3f
import java.lang.IllegalArgumentException
import kotlin.jvm.JvmOverloads

/**
 * Constructs a 4-sided pyramid [Node] with side length given in [sizes] and height given in [height]
 *
 * @author Konrad Michel <konrad.michel@mailbox.tu-dresden.de>
 * @property[sizes] The side length of the pyramids bottom plane
 * @property[height] The height of the pyramid
 */
open class Pyramid @JvmOverloads constructor(val sizes: Vector2f = Vector2f(1.0f, 1.0f), val height : Float, val insideNormals: Boolean = false) : Mesh("box") {
    init {
        val side = 1.0f
        val side2 = side / 2.0f

        boundingBox = OrientedBoundingBox(this,
            -side2 * sizes.x(),
            0.0f,
            -side2 * sizes.y(),
            side2 * sizes.x(),
            height,
            side2 * sizes.y())

        geometry {

            vertices = BufferUtils.allocateFloatAndPut(floatArrayOf(
                // Bottom
                -sizes.x() * side2, 0.0f,-side2*sizes.y(),
                sizes.x() * side2, 0.0f, -side2*sizes.y(),
                -sizes.x() * side2, 0.0f, side2*sizes.y(),
                sizes.x() * side2, 0.0f, +side2*sizes.y(),

                // Front
                -sizes.x() * side2, 0.0f, side2*sizes.y(),
                sizes.x() * side2, 0.0f, +side2*sizes.y(),
                0.0f, height, 0.0f,

                // Right
                sizes.x() * side2, 0.0f, +side2*sizes.y(),
                sizes.x() * side2, 0.0f, -side2*sizes.y(),
                0.0f, height, 0.0f,

                // Back
                sizes.x() * side2, 0.0f, -side2*sizes.y(),
                -sizes.x() * side2, 0.0f,-side2*sizes.y(),
                0.0f, height, 0.0f,

                // Left
                -sizes.x() * side2, 0.0f,-side2*sizes.y(),
                -sizes.x() * side2, 0.0f, side2*sizes.y(),
                0.0f, height, 0.0f
            ))

            val flip: Float = if(insideNormals) { -1.0f } else { 1.0f }

            val frontRight = Vector3f(sizes.x() * side2, 0.0f, side2*sizes.y())
            val frontLeft = Vector3f(-sizes.x() * side2, 0.0f, side2*sizes.y())
            val backRight = Vector3f(sizes.x() * side2, 0.0f, -side2*sizes.y())
            val top = Vector3f(0.0f, height, 0.0f)
            val backLeft = Vector3f(-sizes.x() * side2, 0.0f,-side2*sizes.y())

            val frontNormal = (frontLeft-frontRight).cross(top-frontRight)
            val rightNormal = (backRight-frontRight).cross(top-frontRight)
            val backNormal = (backRight-backLeft).cross(top-backLeft)
            val leftNormal = (frontLeft-backLeft).cross(top-backLeft)

            normals = BufferUtils.allocateFloatAndPut(floatArrayOf(
                // Bottom
                0.0f, -1.0f*flip, 0.0f,
                0.0f, -1.0f*flip, 0.0f,
                0.0f, -1.0f*flip, 0.0f,
                0.0f, -1.0f*flip, 0.0f,
                // Front
                flip*frontNormal.x, flip*frontNormal.y, flip*frontNormal.z,
                flip*frontNormal.x, flip*frontNormal.y, flip*frontNormal.z,
                flip*frontNormal.x, flip*frontNormal.y, flip*frontNormal.z,
                // Right
                flip*rightNormal.x, flip*rightNormal.y, flip*rightNormal.z,
                flip*rightNormal.x, flip*rightNormal.y, flip*rightNormal.z,
                flip*rightNormal.x, flip*rightNormal.y, flip*rightNormal.z,
                // Back
                flip*backNormal.x, flip*backNormal.y, flip*backNormal.z,
                flip*backNormal.x, flip*backNormal.y, flip*backNormal.z,
                flip*backNormal.x, flip*backNormal.y, flip*backNormal.z,
                // Left
                flip*leftNormal.x, flip*leftNormal.y, flip*leftNormal.z,
                flip*leftNormal.x, flip*leftNormal.y, flip*leftNormal.z,
                flip*leftNormal.x, flip*leftNormal.y, flip*leftNormal.z

            ))
            indices = BufferUtils.allocateIntAndPut(intArrayOf(
                0, 1, 2, 2, 1, 3,
                4, 5, 6,
                7, 8, 9,
                10, 11, 12,
                13, 14, 15
            ))
            texcoords = BufferUtils.allocateFloatAndPut(floatArrayOf(
                //Bottom
                0.0f, 0.0f,
                1.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f,
                //Front
                0.0f, 0.0f,
                1.0f, 0.0f,
                1.0f, 1.0f,
                //Right
                0.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 0.0f,
                //Back
                1.0f, 1.0f,
                0.0f, 1.0f,
                0.0f, 0.0f,
                //Left
                1.0f, 0.0f,
                1.0f, 1.0f,
                0.0f, 1.0f
            ))
        }

        boundingBox = generateBoundingBox()
    }

    override fun getConstructorParameters(): Any? {
        return sizes to insideNormals
    }

    override fun constructWithParameters(parameters: Any, hub: Hub): Networkable {
        val pair = parameters as Pair<*,*>
        val sizes = pair.first as? Vector3f
        val insideNormals = pair.second as? Boolean
        if (sizes == null || insideNormals == null){
            throw IllegalArgumentException()
        }
        return Box(sizes,insideNormals)
    }

    companion object {
        /**
         * Creates a box with a hull of size [outerSize] and a wall thickness given in [wallThickness].
         * Returns a container node containing both.
         */
        @JvmStatic fun hulledBox(outerSize: Vector3f = Vector3f(1.0f, 1.0f, 1.0f), wallThickness: Float = 0.05f): Mesh {
            val container = Mesh()
            val outer = Box(outerSize, insideNormals = false)
            container.addChild(outer)

            val innerSize = outerSize - Vector3f(1.0f, 1.0f, 1.0f) * wallThickness * 0.5f
            val inner = Box(innerSize, insideNormals = true)
            inner.material {
                cullingMode = Material.CullingMode.Front
            }
            container.addChild(inner)

            return container
        }
    }
}
