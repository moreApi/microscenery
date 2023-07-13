package microscenery.primitives

import graphics.scenery.*
import graphics.scenery.net.Networkable
import graphics.scenery.utils.extensions.*
import org.joml.Vector3f

/**
 * Constructs a 4-sided pyramid [Node] with side length given in [sizes] and height given in [height]
 *
 * @author Konrad Michel <konrad.michel@mailbox.tu-dresden.de>
 * @property[bottomWidth] The width of the pyramids bottom plane
 * @property[bottomDepth] The depth of the pyramids bottom plane
 * @property[height] The height of the pyramid
 */
@Suppress("LeakingThis")
open class Pyramid @JvmOverloads constructor(
    val bottomWidth: Float = 1f,
    val bottomDepth: Float = 1f,
    val height: Float = 1f,
    val insideNormals: Boolean = false
) : Mesh("box") {
    init {
        val side = 1.0f
        val side2 = side / 2.0f

        boundingBox = OrientedBoundingBox(
            this,
            -side2 * bottomWidth,
            0.0f,
            -side2 * bottomDepth,
            side2 * bottomWidth,
            height,
            side2 * bottomDepth
        )

        geometry {

            vertices = BufferUtils.allocateFloatAndPut(
                floatArrayOf(
                    // Bottom
                    -bottomWidth * side2, 0.0f, -side2 * bottomDepth,
                    bottomWidth * side2, 0.0f, -side2 * bottomDepth,
                    -bottomWidth * side2, 0.0f, side2 * bottomDepth,
                    bottomWidth * side2, 0.0f, +side2 * bottomDepth,

                    // Front
                    -bottomWidth * side2, 0.0f, side2 * bottomDepth,
                    bottomWidth * side2, 0.0f, +side2 * bottomDepth,
                    0.0f, height, 0.0f,

                    // Right
                    bottomWidth * side2, 0.0f, +side2 * bottomDepth,
                    bottomWidth * side2, 0.0f, -side2 * bottomDepth,
                    0.0f, height, 0.0f,

                    // Back
                    bottomWidth * side2, 0.0f, -side2 * bottomDepth,
                    -bottomWidth * side2, 0.0f, -side2 * bottomDepth,
                    0.0f, height, 0.0f,

                    // Left
                    -bottomWidth * side2, 0.0f, -side2 * bottomDepth,
                    -bottomWidth * side2, 0.0f, side2 * bottomDepth,
                    0.0f, height, 0.0f
                )
            )

            val flip: Float = if (insideNormals) {
                -1.0f
            } else {
                1.0f
            }

            val frontRight = Vector3f(bottomWidth * side2, 0.0f, side2 * bottomDepth)
            val frontLeft = Vector3f(-bottomWidth * side2, 0.0f, side2 * bottomDepth)
            val backRight = Vector3f(bottomWidth * side2, 0.0f, -side2 * bottomDepth)
            val top = Vector3f(0.0f, height, 0.0f)
            val backLeft = Vector3f(-bottomWidth * side2, 0.0f, -side2 * bottomDepth)

            val frontNormal = ((top - frontRight).cross(frontLeft - frontRight)).normalize()
            val rightNormal = ((backRight - frontRight).cross(top - frontRight)).normalize()
            val backNormal = ((top - backLeft).cross(backRight - backLeft)).normalize()
            val leftNormal = ((frontLeft - backLeft).cross(top - backLeft)).normalize()

            normals = BufferUtils.allocateFloatAndPut(
                floatArrayOf(
                    // Bottom
                    0.0f, -1.0f * flip, 0.0f,
                    0.0f, -1.0f * flip, 0.0f,
                    0.0f, -1.0f * flip, 0.0f,
                    0.0f, -1.0f * flip, 0.0f,
                    // Front
                    flip * frontNormal.x, flip * frontNormal.y, flip * frontNormal.z,
                    flip * frontNormal.x, flip * frontNormal.y, flip * frontNormal.z,
                    flip * frontNormal.x, flip * frontNormal.y, flip * frontNormal.z,
                    // Right
                    flip * rightNormal.x, flip * rightNormal.y, flip * rightNormal.z,
                    flip * rightNormal.x, flip * rightNormal.y, flip * rightNormal.z,
                    flip * rightNormal.x, flip * rightNormal.y, flip * rightNormal.z,
                    // Back
                    flip * backNormal.x, flip * backNormal.y, flip * backNormal.z,
                    flip * backNormal.x, flip * backNormal.y, flip * backNormal.z,
                    flip * backNormal.x, flip * backNormal.y, flip * backNormal.z,
                    // Left
                    flip * leftNormal.x, flip * leftNormal.y, flip * leftNormal.z,
                    flip * leftNormal.x, flip * leftNormal.y, flip * leftNormal.z,
                    flip * leftNormal.x, flip * leftNormal.y, flip * leftNormal.z

                )
            )
            indices = BufferUtils.allocateIntAndPut(
                intArrayOf(
                    0, 1, 2, 2, 1, 3,
                    4, 5, 6,
                    7, 8, 9,
                    10, 11, 12,
                    13, 14, 15
                )
            )
            texcoords = BufferUtils.allocateFloatAndPut(
                floatArrayOf(
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
                )
            )
        }

        boundingBox = generateBoundingBox()
    }

    override fun getConstructorParameters(): Any? {
        return Vector3f(bottomWidth, height, bottomDepth) to insideNormals
    }

    override fun constructWithParameters(parameters: Any, hub: Hub): Networkable {
        val pair = parameters as Pair<*, *>
        val sizes = pair.first as? Vector3f
        val insideNormals = pair.second as? Boolean
        if (sizes == null || insideNormals == null) {
            throw IllegalArgumentException()
        }
        return Pyramid(sizes.x, sizes.z, sizes.y, insideNormals)
    }
}
