package microscenery.simulation

import graphics.scenery.numerics.OpenSimplexNoise
import graphics.scenery.volumes.Volume.Companion.generateProceduralVolume
import org.joml.Vector2i
import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random


class Procedural(
    val size: Int = 200,
    seed: Long = Random.nextLong(),
    val use16bit: Boolean = false,
    val radius: Float = size * 0.95f
) {

    val f = 3.0f / size
    val center = 0f //size / 2.0f + 0.5f
    val noise = OpenSimplexNoise(seed)
    val range: Int
    val bytesPerVoxel: Int

    init {
        if (use16bit) {
            range = 65535
            bytesPerVoxel = 2
        } else {
            range = 255
            bytesPerVoxel = 1
        }
    }

    /**
     * Algorithm copied from [Volume.generateProceduralVolume]
     */
    fun slice(pos: Vector3f, imgSize: Vector2i): ByteBuffer {

        val byteSize = (imgSize.x * imgSize.y * bytesPerVoxel)

        val buffer = MemoryUtil.memAlloc(byteSize)
        val shortBufferView = buffer.asShortBuffer()

        for (y in pos.y.toInt() until pos.y.toInt() + imgSize.y)
            for (x in pos.x.toInt() until pos.x.toInt() + imgSize.x) {
                val z = pos.z

                val result = point(Vector3f(x.toFloat(), y.toFloat(), z))

                if (use16bit) {
                    shortBufferView.put(result)
                } else {
                    buffer.put(result.toByte())
                }
            }
        buffer.clear()
        return buffer
    }

    fun point(pos: Vector3f): Short {
        val dx = center - pos.x
        val dy = center - pos.y
        val dz = center - pos.z

        val offset = abs(noise.random3D((pos.x) * f, (pos.y) * f, (pos.z) * f))
        val d = sqrt(dx * dx + dy * dy + dz * dz) / size

        val result = if (radius > Math.ulp(1.0f)) {
            if (d - offset < radius) {
                ((d - offset) * range).toInt().toShort()
            } else {
                0
            }
        } else {
            ((d - offset) * range).toInt().toShort()
        }
        return result
    }
}