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
import graphics.scenery.backends.Renderer
import graphics.scenery.backends.Shaders
import graphics.scenery.primitives.Line
import graphics.scenery.textures.Texture
import graphics.scenery.volumes.Colormap
import graphics.scenery.volumes.TransferFunction
import graphics.scenery.volumes.VolumeHistogramComputeNode
import net.imglib2.type.numeric.integer.UnsignedByteType
import net.imglib2.type.numeric.integer.UnsignedShortType
import net.imglib2.type.numeric.real.FloatType
import org.jfree.data.statistics.SimpleHistogramDataset
import org.joml.Vector3f
import org.joml.Vector3i
import org.joml.Vector4f
import java.nio.ByteBuffer

/**
 * Modified Plane to display ByteBuffers
 */
class SliceRenderNode(
    val slice: ByteBuffer, val width: Int,val height: Int, scale: Float = 1f, var bytesPerValue: Int = 1,
    transferFunction: TransferFunction, minDisplayRange: Float? = null, maxDisplayRange: Float? = null,
    colormap: Colormap? = null
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

    var minDisplayRange: Float = minDisplayRange ?:0f
        set(value) {
            field = value
            calculateOffsetAndScale()
        }
    var maxDisplayRange: Float = maxDisplayRange ?: when(bytesPerValue)  {
        1 -> 255f
        2 -> 65535f
        else -> throw IllegalArgumentException()
    }
        set(value) {
            field = value
            calculateOffsetAndScale()
        }

    var colormap = colormap ?: Colormap.get("hot")
        set(value) {
            field = value
            updateColorMapTexture()
        }

    /**
     * This normally happens inside the converter of a volume.
     * Converts the minDisplayRange and maxDisplayRange values into an offset and scale used inside the shader
     * and saves it to material properties
     */
    private fun calculateOffsetAndScale() {
        // Rangescale is either 255 or 65535
        val rangeScale = when(bytesPerValue)  {
            1 -> 255
            2 -> 65535
            else -> throw IllegalArgumentException()
        }
        val fmin = minDisplayRange / rangeScale
        val fmax = maxDisplayRange / rangeScale
        val transferFunctionScale = 1.0f / (fmax - fmin)
        material().roughness = transferFunctionScale
        val transferFunctionOffset = -fmin * transferFunctionScale
        material().metallic = transferFunctionOffset
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
        transferFunctionTexture = generateTFTexture()

        val side = 1.0f
        val side2 = side / 2.0f

        geometry {
            vertices = BufferUtils.allocateFloatAndPut(
                floatArrayOf(
                    // Front
                    -side2, -side2, 0.0f,
                    side2, -side2, 0.0f,
                    side2, side2, 0.0f,
                    -side2, side2, 0.0f
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
            updateColorMapTexture()
            calculateOffsetAndScale()

            blending.sourceColorBlendFactor = Blending.BlendFactor.SrcAlpha
            blending.destinationColorBlendFactor = Blending.BlendFactor.OneMinusSrcAlpha
            blending.colorBlending = Blending.BlendOp.add
            blending.sourceAlphaBlendFactor = Blending.BlendFactor.One
            blending.destinationAlphaBlendFactor = Blending.BlendFactor.OneMinusSrcAlpha
            blending.alphaBlending = Blending.BlendOp.add
        }
    }

    private fun updateColorMapTexture() {
        material().textures["ambient"] = Texture(
            dimensions = Vector3i(colormap.width, colormap.height, 1),
            channels = 4,
            type = UnsignedByteType(),
            contents = colormap.buffer,
            repeatUVW = Texture.RepeatMode.ClampToBorder.all(),
            borderColor = Texture.BorderColor.TransparentBlack,
            normalized = true,
            mipmap = false,
            minFilter = Texture.FilteringMode.NearestNeighbour,
            maxFilter = Texture.FilteringMode.NearestNeighbour,
            usageType = hashSetOf(Texture.UsageType.Texture)
        )
    }

    /**
     * Adds or removes the border according to [visibility]
     */
    fun setBorderVisibility(visibility : Boolean)
    {
        if(visibility)
        {
            val border = Line(8, false, true)
            border.name = "Border"

            val side = 1.0f
            val side2 = side / 2.0f
            val color = Vector4f(0.0f, 0.0f, 0.0f, 1.0f)
            border.lineColor = color
            border.startColor = color
            border.endColor = color
            border.edgeWidth = 4f

            border.addPoint(Vector3f(-side2, -side2, 0.0f))
            border.addPoint(Vector3f(side2, -side2, 0.0f))
            border.addPoint(Vector3f(side2, -side2, 0.0f))
            border.addPoint(Vector3f(side2, side2, 0.0f))
            border.addPoint(Vector3f(side2, side2, 0.0f))
            border.addPoint(Vector3f(-side2, side2, 0.0f))
            border.addPoint(Vector3f(-side2, side2, 0.0f))
            border.addPoint(Vector3f(-side2, -side2, 0.0f))
            this.addChild(border)
        }
        else
        {
            val border = this.getChildrenByName("Border").first()
            this.removeChild(border)
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

    /*
    // inverse of [TransferFunctionManager.calculateOffsetAndScale()]
    private fun calculateDisplayRange() {
        // Rangescale is either 255 or 65535
        val rangeScale = when(bytesPerValue)  {
            1 -> 255
            2 -> 65535
            else -> throw IllegalArgumentException()
        }
        //val fmin = minDisplayRange / rangeScale
        //val fmax = maxDisplayRange / rangeScale
        val fmin = -1* transferFunctionOffset / transferFunctionScale
        val fmax = (1f - transferFunctionScale * fmin) / transferFunctionScale
        displayRangeMin = fmin * rangeScale
        displayRangeMax = fmax * rangeScale
    }*/

    /**
     * Generates a histogram using GPU acceleration via [VolumeHistogramComputeNode].
     */
    fun generateHistogram(volumeHistogramData: SimpleHistogramDataset, renderer: Renderer): Int {
        slice.rewind()

        return  VolumeHistogramComputeNode.generateHistogram(
            minDisplayRange to maxDisplayRange,
            Vector3i(width, height/2, 2), //VolumeHistogramComputeNode cant handle z=1 therefor we cheat a bit with the dimensions
            bytesPerValue,
            this.getScene()!!,
            slice,
            renderer,
            volumeHistogramData
        )
    }
}