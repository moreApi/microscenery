package microscenery

import graphics.scenery.BufferUtils
import graphics.scenery.DefaultNode
import graphics.scenery.ShaderMaterial
import graphics.scenery.attribute.geometry.HasGeometry
import graphics.scenery.attribute.material.HasCustomMaterial
import graphics.scenery.attribute.material.Material
import graphics.scenery.attribute.renderable.HasRenderable
import graphics.scenery.attribute.spatial.HasSpatial
import graphics.scenery.textures.Texture
import graphics.scenery.utils.Image
import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.ShortBuffer

/**
 * Modified Plane to display ByteBuffers
 */
class SliceRenderNode(slice: ByteBuffer, width: Int, height: Int, scale: Float = 1f, bytesPerValue: Int = 1) : DefaultNode("SliceRenderNode"),
    HasSpatial, HasRenderable,
    HasCustomMaterial<ShaderMaterial>, HasGeometry {

    init {

        val sizes = Vector3f(width * scale, height * scale, 1f)

        addGeometry()
        addRenderable()
        addMaterial()
        addSpatial()

        spatial {
            this.scale = sizes
        }

        val side = 1.0f
        val side2 = side / 2.0f

        geometry {
            vertices = BufferUtils.allocateFloatAndPut(
                floatArrayOf(
                    // Front
                    -side2, -side2, side2,
                    side2, -side2, side2,
                    side2, side2, side2,
                    -side2, side2, side2
                )
            )

            normals = BufferUtils.allocateFloatAndPut(
                floatArrayOf(
                    // Front
                    0.0f, 0.0f, 1.0f,
                    0.0f, 0.0f, 1.0f,
                    0.0f, 0.0f, 1.0f,
                    0.0f, 0.0f, 1.0f
                )
            )
            indices = BufferUtils.allocateIntAndPut(
                intArrayOf(
                    0, 1, 2, 0, 2, 3
                )
            )

            texcoords = BufferUtils.allocateFloatAndPut(
                floatArrayOf(
                    0.0f, 0.0f,
                    1.0f, 0.0f,
                    1.0f, 1.0f,
                    0.0f, 1.0f
                )
            )
        }

        boundingBox = generateBoundingBox()


        this.material().cullingMode = Material.CullingMode.None

        val bufferSize = width * height
        val dataExpaned = MemoryUtil.memAlloc((bufferSize * 4))
        val data = when(bytesPerValue){
            1 -> slice
            2 -> slice.asShortBuffer()
            else -> throw IllegalArgumentException("bytesPerValue: $bytesPerValue not valid")
        }

        for ( i in 1..bufferSize){
            val v = when(data) {
                is ByteBuffer -> data.get()
                is ShortBuffer -> data.get().toByte()
                else -> throw IllegalStateException()
            }

            dataExpaned.put(v)
            dataExpaned.put(v)
            dataExpaned.put(v)
            dataExpaned.put(Byte.MAX_VALUE)

        }
        dataExpaned.rewind()

        val final = Image(dataExpaned, width, height)
        this.material {
            textures["diffuse"] = Texture.fromImage(final)
        }
    }

    override fun createMaterial(): ShaderMaterial {

        val newMaterial: ShaderMaterial = ShaderMaterial.fromFiles(
            "DefaultForward.vert",
            "DefaultForward.frag",
        )

        setMaterial(newMaterial) {
            newMaterial.diffuse = diffuse
            newMaterial.specular = specular
            newMaterial.ambient = ambient
            newMaterial.metallic = metallic
            newMaterial.roughness = roughness
            newMaterial.blending.transparent = true

            cullingMode = Material.CullingMode.None
        }

        return newMaterial
    }
}