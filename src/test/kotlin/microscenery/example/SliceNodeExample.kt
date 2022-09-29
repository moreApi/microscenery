package microscenery.example

import graphics.scenery.volumes.Volume
import io.scif.util.FormatTools
import microscenery.DefaultScene
import microscenery.MMConnection
import microscenery.SliceRenderNode
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

class SliceRenderFromFileExample : DefaultScene() {
    override fun init() {
        super.init()

        val (data, dims) = fromPath(
            Path("""C:\Users\JanCasus\volumes\unpublished\cherry_slice_55_tf_applied_RGB.tif""")
        )




        SliceRenderNode(
            data,
            dims[0],
            dims[1],
            0.005f,
            1
        )
            .apply { scene.addChild(this) }


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
        logger.debug("${file.fileName}: Allocated ${imageData.capacity()} bytes for $type ${8 * bytesPerVoxel}bit image of $dims")

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

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SliceRenderFromFileExample().main()
        }
    }
}

class SliceNodeMMExample() : DefaultScene() {

    override fun init() {
        super.init()


        val mmConnection = MMConnection()

        for (i in 0..100) {
            val buffer = MemoryUtil.memAlloc(mmConnection.height * mmConnection.width * 2)//shortType
            mmConnection.moveStage(Vector3f(0f, 0f, i.toFloat()), false)
            mmConnection.snapSlice(buffer.asShortBuffer())
            SliceRenderNode(buffer, mmConnection.width, mmConnection.height, 0.005f, 2).let {
                scene.addChild(it)
                it.spatial().position = Vector3f(i.toFloat())
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SliceNodeMMExample().main()
        }
    }
}

