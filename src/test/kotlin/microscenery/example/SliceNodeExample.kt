package microscenery.example

import graphics.scenery.textures.Texture
import graphics.scenery.attribute.material.Material
import graphics.scenery.primitives.Plane
import graphics.scenery.utils.Image
import graphics.scenery.volumes.Volume
import io.scif.util.FormatTools
import microscenery.DefaultScene
import net.imglib2.type.numeric.NumericType
import net.imglib2.type.numeric.integer.*
import net.imglib2.type.numeric.real.FloatType
import org.joml.Vector3f
import org.joml.Vector3i
import org.lwjgl.system.MemoryUtil
import org.scijava.io.location.FileLocation
import java.nio.ByteBuffer
import java.nio.file.Path
import kotlin.io.path.Path

class SliceRenderExample: DefaultScene() {
    override fun init() {
        super.init()

        val (data, dims) = fromPath(
            Path("""C:\Users\JanCasus\volumes\unpublished\cherry_slice_55_tf_applied_RGB.tif""")
        )

        val dataExpaned = MemoryUtil.memAlloc((data.capacity() * 4))
        for ( i in 1..data.capacity()){
            val v = data.get()
            dataExpaned.put(v)
            dataExpaned.put(v)
            dataExpaned.put(v)
            dataExpaned.put(Byte.MAX_VALUE)
        }
        dataExpaned.rewind()


        val plane = Plane(
            Vector3f(-0.5f,-0.5f,0f),
            Vector3f(-0.5f,0.5f,0f),
            Vector3f(0.5f,-0.5f,0f),
            Vector3f(0.5f,0.5f,0f))
            .apply { scene.addChild(this) }


        plane.material().cullingMode = Material.CullingMode.None


        val final = Image(dataExpaned, dims[0], dims[1])
        plane.material {
            textures["diffuse"] = Texture.fromImage(final)
        }

    }

    fun fromPath(file: Path): Pair<ByteBuffer, Vector3i> {

        val id = file.fileName.toString()

        val reader = Volume.scifio.initializer().initializeReader(FileLocation(file.toFile()))

        val dims = Vector3i()
        with(reader.openPlane(0, 0)) {
            dims.x = lengths[0].toInt()
            dims.y = lengths[1].toInt()
            dims.z = reader.getPlaneCount(0).toInt()
        }

        val bytesPerVoxel = reader.openPlane(0, 0).imageMetadata.bitsPerPixel / 8
        reader.openPlane(0, 0).imageMetadata.pixelType

        val type: NumericType<*> = when (reader.openPlane(0, 0).imageMetadata.pixelType) {
            FormatTools.INT8 -> ByteType()
            FormatTools.INT16 -> ShortType()
            FormatTools.INT32 -> IntType()

            FormatTools.UINT8 -> UnsignedByteType()
            FormatTools.UINT16 -> UnsignedShortType()
            FormatTools.UINT32 -> UnsignedIntType()

            FormatTools.FLOAT -> FloatType()

            else -> {
                logger.error(
                    "Unknown scif.io pixel type ${
                        reader.openPlane(
                            0,
                            0
                        ).imageMetadata.pixelType
                    }, assuming unsigned byte."
                )
                UnsignedByteType()
            }
        }

        logger.debug("Loading $id from disk")
        val imageData: ByteBuffer = MemoryUtil.memAlloc((bytesPerVoxel * dims.x * dims.y * dims.z))
        logger.debug("${file.fileName}: Allocated ${imageData.capacity()} bytes for $type ${8*bytesPerVoxel}bit image of $dims")

        val start = System.nanoTime()

//            if(reader.openPlane(0, 0).imageMetadata.isLittleEndian) {
        logger.debug("Volume is little endian")
        (0 until reader.getPlaneCount(0)).forEach { plane ->
            imageData.put(reader.openPlane(0, plane).bytes)
        }
//            } else {
//                logger.info("Volume is big endian")
//                (0 until reader.getPlaneCount(0)).forEach { plane ->
//                    imageData.put(swapEndianUnsafe(reader.openPlane(0, plane).bytes))
//                }
//            }

        val duration = (System.nanoTime() - start) / 10e5
        logger.debug("Reading took $duration ms")

        imageData.flip()
        return imageData to dims
    }
    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            SliceRenderExample().main()
        }
    }
}