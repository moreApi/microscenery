package microscenery.stageSpace

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
import graphics.scenery.volumes.Colormap
import graphics.scenery.volumes.TransferFunction
import net.imglib2.type.numeric.integer.UnsignedByteType
import net.imglib2.type.numeric.integer.UnsignedShortType
import net.imglib2.type.numeric.real.FloatType
import org.joml.Vector3f
import org.joml.Vector3i
import java.nio.ByteBuffer

/**
 * Modified Plane to display ByteBuffers
 */
class SliceRenderNode(
    slice: ByteBuffer, width: Int, height: Int, scale: Float = 1f, var bytesPerValue: Int = 1,
    transferFunction: TransferFunction, tfOffset: Float = 0.0f, tfScale: Float? = null
) : DefaultNode("SliceRenderNode"),
    HasSpatial, HasRenderable,
    HasCustomMaterial<ShaderMaterial>, HasGeometry {

    private var transferFunctionTexture : Texture
    var transferFunction : TransferFunction = transferFunction
        set(value) {
            field = value
            transferFunctionTexture = generateTFTexture()
            material().textures["specular"] = transferFunctionTexture
        }
    var transferFunctionOffset : Float = tfOffset
        set(value) {
            field = value
            material().metallic = field
        }
    var transferFunctionScale : Float = 0.0f
        set(value) {
            field = value
            material().roughness = field
        }

    init {
        val sizes = Vector3f(width * scale, height * scale, 1f)

        addSpatial()
        addMaterial()
        addGeometry()
        addRenderable()

        spatial {
            this.scale = sizes
        }
        transferFunctionScale = tfScale
            ?: when (bytesPerValue) {
                1 -> 0.255f
                2 -> 65.5f
                else -> throw IllegalArgumentException("bytesPerValue: $bytesPerValue not valid")
            }
        transferFunctionTexture = generateTFTexture()

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

        val colorMap = Colormap.get("hot")

        this.material {
            // data
            textures["diffuse"] = Texture(
                dimensions = Vector3i(width, height, 1),
                channels = 1,
                type = when (bytesPerValue) {
                    1 -> UnsignedByteType()
                    2 -> UnsignedShortType()
                    else -> throw IllegalArgumentException("bytesPerValue: $bytesPerValue not valid")
                },
                contents = slice,
                repeatUVW = Texture.RepeatMode.ClampToBorder.all(),
                borderColor = Texture.BorderColor.TransparentBlack,
                normalized = true,
                mipmap = true,
                minFilter = Texture.FilteringMode.NearestNeighbour,
                maxFilter = Texture.FilteringMode.NearestNeighbour,
                usageType = hashSetOf(Texture.UsageType.Texture)
            )
            // transfer function
            textures["specular"] = transferFunctionTexture
            // color map
            textures["ambient"] = Texture(
                dimensions = Vector3i(colorMap.width, colorMap.height, 1),
                channels = 4,
                type = UnsignedByteType(),
                contents = colorMap.buffer,
                repeatUVW = Texture.RepeatMode.ClampToBorder.all(),
                borderColor = Texture.BorderColor.TransparentBlack,
                normalized = true,
                mipmap = false,
                minFilter = Texture.FilteringMode.NearestNeighbour,
                maxFilter = Texture.FilteringMode.NearestNeighbour,
                usageType = hashSetOf(Texture.UsageType.Texture)
            )
            metallic = transferFunctionOffset
            roughness = transferFunctionScale

            blending.sourceColorBlendFactor = Blending.BlendFactor.SrcAlpha
            blending.destinationColorBlendFactor = Blending.BlendFactor.OneMinusSrcAlpha
            blending.colorBlending = Blending.BlendOp.add
            blending.sourceAlphaBlendFactor = Blending.BlendFactor.One
            blending.destinationAlphaBlendFactor = Blending.BlendFactor.OneMinusSrcAlpha
            blending.alphaBlending = Blending.BlendOp.add
        }
    }

    private fun generateTFTexture() : Texture {
        return Texture(
            dimensions = Vector3i(transferFunction.textureSize, transferFunction.textureHeight, 1),
            channels = 1,
            type = FloatType(),
            contents = transferFunction.serialise(),
            repeatUVW = Texture.RepeatMode.ClampToBorder.all(),
            borderColor = Texture.BorderColor.TransparentBlack,
            normalized = false,
            mipmap = false,
            minFilter = Texture.FilteringMode.NearestNeighbour,
            maxFilter = Texture.FilteringMode.NearestNeighbour,
            usageType = hashSetOf(Texture.UsageType.Texture)
        )
    }

    override fun createMaterial(): ShaderMaterial {

        val shaders = Shaders.ShadersFromFiles(
            arrayOf(
                "DefaultForward.vert",
                "SliceRenderNode.frag"
            ), SliceRenderNode::class.java
        )

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