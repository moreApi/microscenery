package microscenery

import graphics.scenery.Blending
import graphics.scenery.BufferUtils
import graphics.scenery.DefaultNode
import graphics.scenery.ShaderMaterial
import graphics.scenery.attribute.geometry.HasGeometry
import graphics.scenery.attribute.material.HasCustomMaterial
import graphics.scenery.attribute.material.Material
import graphics.scenery.attribute.renderable.HasRenderable
import graphics.scenery.attribute.spatial.HasSpatial
import graphics.scenery.backends.Shaders
import graphics.scenery.textures.Texture
import graphics.scenery.utils.Image
import graphics.scenery.volumes.Colormap
import graphics.scenery.volumes.TransferFunction
import net.imglib2.type.numeric.NumericType
import net.imglib2.type.numeric.integer.UnsignedByteType
import net.imglib2.type.numeric.integer.UnsignedIntType
import net.imglib2.type.numeric.real.FloatType
import org.joml.Vector3f
import org.joml.Vector3i
import org.lwjgl.system.MemoryUtil
import java.awt.image.BufferedImage
import java.io.File
import java.nio.ByteBuffer
import java.nio.ShortBuffer
import javax.imageio.ImageIO

/**
 * Modified Plane to display ByteBuffers
 */
class SliceRenderNode(slice: ByteBuffer, width: Int, height: Int, scale: Float = 1f, bytesPerValue: Int = 1,
    tf : TransferFunction) : DefaultNode("SliceRenderNode"),
    HasSpatial, HasRenderable,
    HasCustomMaterial<ShaderMaterial>, HasGeometry {

    var transferFunction : TransferFunction = tf
        set(value)
        {
            field = value
            tfTexture = generateTFTexture()
            material().textures["specular"] = tfTexture
        }

    private var tfTexture : Texture
    init {
        val sizes = Vector3f(width * scale, height * scale, 1f)

        addGeometry()
        addRenderable()
        addMaterial()
        addSpatial()

        spatial {
            this.scale = sizes
        }

        tfTexture = generateTFTexture()

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
        val colorMap = Colormap.get("hot")

        this.material {
            textures["diffuse"] = Texture.fromImage(final)
            textures["specular"] = tfTexture
            textures["ambient"] = Texture(Vector3i(colorMap.width, colorMap.height, 1), 4, UnsignedByteType(), colorMap.buffer,
                Texture.RepeatMode.ClampToBorder.all(), Texture.BorderColor.TransparentBlack, true, false,
                Texture.FilteringMode.NearestNeighbour, Texture.FilteringMode.NearestNeighbour,
                hashSetOf(Texture.UsageType.Texture))

            blending.sourceColorBlendFactor = Blending.BlendFactor.SrcAlpha
            blending.destinationColorBlendFactor = Blending.BlendFactor.OneMinusSrcAlpha
            blending.colorBlending = Blending.BlendOp.add
            blending.sourceAlphaBlendFactor = Blending.BlendFactor.One
            blending.destinationAlphaBlendFactor = Blending.BlendFactor.OneMinusSrcAlpha
            blending.alphaBlending = Blending.BlendOp.add
        }
    }



    private fun generateTFTexture() : Texture {
        val tfSerialized = transferFunction.serialise()
        return Texture(Vector3i(transferFunction.textureSize, transferFunction.textureHeight, 1), 1, FloatType(), tfSerialized,
            Texture.RepeatMode.ClampToBorder.all(), Texture.BorderColor.TransparentBlack, true, false,
            Texture.FilteringMode.NearestNeighbour, Texture.FilteringMode.NearestNeighbour,
            hashSetOf(Texture.UsageType.Texture))
    }

    override fun createMaterial(): ShaderMaterial {

        val shaders = Shaders.ShadersFromFiles(arrayOf("DefaultForward.vert",
            "SliceRenderNode.frag"), SliceRenderNode::class.java )

        val newMaterial = ShaderMaterial(shaders)

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