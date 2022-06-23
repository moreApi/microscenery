package microscenery.example


import bdv.util.AxisOrder
import graphics.scenery.*
import graphics.scenery.utils.LazyLogger
import graphics.scenery.volumes.BufferedVolume
import graphics.scenery.volumes.TransferFunction
import graphics.scenery.volumes.Volume
import ij.IJ
import ij.ImagePlus
import io.scif.config.SCIFIOConfig
import io.scif.config.SCIFIOConfig.ImgMode
import io.scif.img.ImgIOException
import io.scif.img.ImgOpener
import io.scif.util.FormatTools
import microscenery.DefaultScene
import net.imglib2.RandomAccessible
import net.imglib2.RandomAccessibleInterval
import net.imglib2.RealRandomAccessible
import net.imglib2.img.Img
import net.imglib2.img.display.imagej.ImageJFunctions
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory
import net.imglib2.realtransform.AffineTransform3D
import net.imglib2.realtransform.RealViews
import net.imglib2.type.NativeType
import net.imglib2.type.numeric.NumericType
import net.imglib2.type.numeric.RealType
import net.imglib2.type.numeric.integer.*
import net.imglib2.type.numeric.real.FloatType
import net.imglib2.view.Views
import org.joml.Quaternionf
import org.joml.Vector3f
import org.joml.Vector3i
import org.lwjgl.system.MemoryUtil
import org.scijava.io.location.FileLocation
import tpietzsch.example2.VolumeViewerOptions
import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.PI
import kotlin.math.sin


class DeskewingExample: DefaultScene({ _, _ -> }) {
    lateinit var volume: Volume

    override fun init() {
        super.init()

        val orignal = ("""C:\Users\JanCasus\volumes\HPO_488nm_05mW_561nm_15mW_50ms_zStack_1\C1-HPO_488nm_05mW_561nm_15mW_50ms_zStack_1_MMStack_Default_oneChannel.ome.ome.tif""")
        val deskewed =  ("""C:\Users\JanCasus\volumes\HPO_488nm_05mW_561nm_15mW_50ms_zStack_1\Deskewed\HPO_488nm_05mW_561nm_15mW_50ms_zStack_1_Deskewed(t=0 p=0 c=0).tif""")
        val maiglocke = """C:\Users\JanCasus\volumes\20220523 maiglocke\RoI_1\RoI_1_MMStack_Default.ome.tif"""
        val maiglocke_desk = """C:\Users\JanCasus\volumes\20220523 maiglocke\RoI_1\RoI_1_MMStack_Default.ome.deskewed.tif"""

        ImageJFunctions.show( openVolume<UnsignedShortType>(maiglocke_desk))

        val imp: ImagePlus = IJ.openImage(maiglocke)
//        val imp: ImagePlus = IJ.openImage(maiglocke_desk)
//        val imp: ImagePlus = IJ.openImage(orignal)
//        val img: Img<UnsignedShortType> = ImageJFunctions.wrapShort(imp)
        val img = openVolume<UnsignedShortType>(maiglocke)
        ImageJFunctions.show( img )
        val input: RandomAccessible<UnsignedShortType> = Views.extendValue(img, UnsignedShortType(0))
        val interpolated: RealRandomAccessible<UnsignedShortType> = Views.interpolate(input, NLinearInterpolatorFactory())


//        val angle_in_degrees = 30.0
//        val voxel_size_y  = 0.178f
//        val voxel_size_z = 0.5
//        val scale_factor = 1.0

        val angle_in_degrees = 12.6
        val voxel_size_y  = 0.698
        val voxel_size_z = 1.0 //0.57
        val scale_factor = 1.0


        val skewM = AffineTransform3D()
        //Defining shear factor
        //Shear factor calculation here is different from that in utilities
        // shear_factor = math.sin((90 - angle_in_degrees) * math.pi / 180.0) * (voxel_size_z / voxel_size_y)
        val shear_factor = sin(Math.toRadians(90.0 - angle_in_degrees)) * (voxel_size_z / voxel_size_y)
        skewM.set(-shear_factor,1,2)


        // make voxels isotropic, calculate the new scaling factor for Z after shearing
        // https://github.com/tlambert03/napari-ndtiffs/blob/092acbd92bfdbf3ecb1eb9c7fc146411ad9e6aae/napari_ndtiffs/affine.py//L57
        // py: new_dz = math.sin(angle_in_degrees * math.pi / 180.0) * voxel_size_z
        // pY: scale_factor_z = (new_dz / voxel_size_y) * scale_factor
        val new_dz = Math.sin(angle_in_degrees * Math.PI / 180.0) * voxel_size_z
        val scale_factor_z = (new_dz / voxel_size_y) * scale_factor
        val scaleM = AffineTransform3D()
        scaleM.scale(scale_factor,scale_factor,scale_factor_z)

        val rotateM = AffineTransform3D()
        // correct orientation so that the new Z-plane goes proximal-distal from the objective.
        // py: self.rotate(angle_in_degrees = 0 - angle_in_degrees, axis=0)
        rotateM.rotate(0,  Math.toRadians(-angle_in_degrees))

        val affine = AffineTransform3D()
            .concatenate(rotateM)
            .concatenate(scaleM)
            .concatenate(skewM)

        // affine.scale(2.3)
        //affine.translate(0.0,img.max(1)/2.0,0.0)

        // as seen viewing along x:
        val topRight =      floatArrayOf(0f, 0f,                       img.max(2).toFloat())
        val topLeft =       floatArrayOf(0f, 0f,                       0f)
        val bottomLeft =    floatArrayOf(0f, img.max(1).toFloat(),  0f)
        val bottomRight =   floatArrayOf(0f, img.max(1).toFloat(),  img.max(2).toFloat())

        val BBdeskewed = listOf(topRight,topLeft,bottomRight,bottomLeft).map {
            val deskewed = FloatArray(3)
            affine.apply(it,deskewed)
            deskewed
        }.toList()

        val min = longArrayOf(
            0,
            BBdeskewed.minOf { it[1].toLong() },
            BBdeskewed.minOf { it[2].toLong() })

        val max = longArrayOf(
            img.max(0),
            BBdeskewed.maxOf { it[1].toLong() },
            BBdeskewed.maxOf { it[2].toLong() })


        val realview: RealRandomAccessible<UnsignedShortType> = RealViews.affineReal(interpolated, affine)
//        val view: RandomAccessibleInterval<UnsignedShortType> = Views.interval(Views.raster(realview), img)
        val view: RandomAccessibleInterval<UnsignedShortType> = Views.interval(Views.raster(realview),
            min,max)
//            longArrayOf(0,deskewedBottomCorner[1].toLong()+1,0),img.dimensionsAsLongArray())


        ImageJFunctions.show( view )


        volume = Volume.fromRAI(view, UnsignedShortType(), AxisOrder.DEFAULT, "T1 head", hub, VolumeViewerOptions())
//        volume = Volume.fromRAI(img, UnsignedShortType(), AxisOrder.DEFAULT, "T1 head", hub, VolumeViewerOptions())
        volume.transferFunction = TransferFunction.ramp(0.00f, 0.5f, 0.3f)
        volume.spatial(){
            //scale = Vector3f(0.1f,0.1f,0.5f,)
            scale = Vector3f(0.2f, 0.2f, 0.2f)
            rotation = Quaternionf()
                .rotateY((PI/2).toFloat()) // now z goes right
//                .rotateX(Math.toRadians(-angle_in_degrees).toFloat())
            //position = Vector3f(0f,-0.5f,0f)
        }
        volume.origin = Origin.FrontBottomLeft
        scene.addChild(volume)

        val bg = BoundingGrid()
        bg.node = volume
        volume.metadata["BoundingGrid"] = bg
        scene.addChild(bg)

        scene.addChild(Sphere(0.05f))

        val plane = Box(Vector3f(0.001f,100f,100f))
        plane.material().diffuse = Vector3f(0.0f,1f,0.1f)
        scene.addChild(plane)


//        thread {
//            while (true){
//                Thread.sleep(200)
//                print(scene)
//            }
//        }

    }

    override fun inputSetup() {
        setupCameraModeSwitching()
    }

    @Throws(ImgIOException::class)
    fun <T> openVolume(path:String): Img<T> where T : RealType<T>?, T : NativeType<T>? {
        // define the file to open
        val file = File(path)
        val path: String = file.getAbsolutePath()

        // create the ImgOpener
        val imgOpener = ImgOpener()

        // open with ImgOpener. The type (e.g. ArrayImg, PlanarImg, CellImg) is
        // automatically determined. For a small image that fits in memory, this
        // should open as an ArrayImg.
        val image = imgOpener.openImgs(path)[0] as Img<T>

        return image
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            DeskewingExample().main()
        }
    }
}
