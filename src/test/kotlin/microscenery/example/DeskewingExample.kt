package microscenery.example


import bdv.util.AxisOrder
import graphics.scenery.BoundingGrid
import graphics.scenery.Origin
import graphics.scenery.Sphere
import graphics.scenery.volumes.TransferFunction
import graphics.scenery.volumes.Volume
import ij.IJ
import ij.ImagePlus
import microscenery.DefaultScene
import net.imglib2.RandomAccessible
import net.imglib2.RandomAccessibleInterval
import net.imglib2.RealRandomAccessible
import net.imglib2.img.Img
import net.imglib2.img.display.imagej.ImageJFunctions
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory
import net.imglib2.realtransform.AffineTransform3D
import net.imglib2.realtransform.RealViews
import net.imglib2.type.numeric.integer.UnsignedShortType
import net.imglib2.view.Views
import org.joml.Quaternionf
import org.joml.Vector3f
import tpietzsch.example2.VolumeViewerOptions
import kotlin.math.PI
import kotlin.math.sin


class DeskewingExample: DefaultScene({ _, _ -> }) {
    lateinit var volume: Volume

    override fun init() {
        super.init()

        val orignal = ("""C:\Users\JanCasus\volumes\HPO_488nm_05mW_561nm_15mW_50ms_zStack_1\C1-HPO_488nm_05mW_561nm_15mW_50ms_zStack_1_MMStack_Default_oneChannel.ome.ome.tif""")
        val deskewed =  ("""C:\Users\JanCasus\volumes\HPO_488nm_05mW_561nm_15mW_50ms_zStack_1\Deskewed\HPO_488nm_05mW_561nm_15mW_50ms_zStack_1_Deskewed(t=0 p=0 c=0).tif""")
        val maiglocke = """C:\Users\JanCasus\volumes\20220523 maiglocke\RoI_1\RoI_1_MMStack_Default.ome.tif"""

        val imp: ImagePlus = IJ.openImage(maiglocke)
//        val imp: ImagePlus = IJ.openImage(orignal)
        val img: Img<UnsignedShortType> = ImageJFunctions.wrapShort(imp)

        val input: RandomAccessible<UnsignedShortType> = Views.extendValue(img, UnsignedShortType(0))
        val interpolated: RealRandomAccessible<UnsignedShortType> = Views.interpolate(input, NLinearInterpolatorFactory())

        val affine = AffineTransform3D()

//        val angle_in_degrees = 30
//        val voxel_size_y  = 0.178f
//        val voxel_size_z = 0.5
//        val scale_factor = 1.0

        val angle_in_degrees = 12.6
        val voxel_size_y  = 0.698
        val voxel_size_z = 1.0 //0.57
        val scale_factor = 1.0


        //Defining shear factor
        //Shear factor calculation here is different from that in utilities
        // shear_factor = math.sin((90 - angle_in_degrees) * math.pi / 180.0) * (voxel_size_z / voxel_size_y)
        val shear_factor = sin(Math.toRadians(90.0 - angle_in_degrees)) * (voxel_size_z / voxel_size_y)
        affine.set(-shear_factor,1,2)


        // make voxels isotropic, calculate the new scaling factor for Z after shearing
        // https://github.com/tlambert03/napari-ndtiffs/blob/092acbd92bfdbf3ecb1eb9c7fc146411ad9e6aae/napari_ndtiffs/affine.py//L57
        val new_dz = Math.sin(angle_in_degrees * Math.PI / 180.0) * voxel_size_z
        val scale_factor_z = (new_dz / voxel_size_y) * scale_factor
        //affine.scale(scale_factor,scale_factor,scale_factor_z)
        //affine.scale(1.0,0.5,1.0)

        // correct orientation so that the new Z-plane goes proximal-distal from the objective.
        //affine.rotate(0,  Math.toRadians(angle_in_degrees.toDouble()))


        // affine.scale(2.3)
        //affine.translate(0.0,img.max(1)/2.0,0.0)

        val topRight = floatArrayOf(0f,0f,img.max(2).toFloat())
        val deskewedTopRight = FloatArray(3)
        affine.apply(topRight,deskewedTopRight)

        val bottomLeft = floatArrayOf(0f,img.max(1).toFloat(),0f)
        val deskewedBottomLeft = FloatArray(3)
        affine.apply(bottomLeft,deskewedBottomLeft)

        val min = longArrayOf(0            ,deskewedTopRight[1].toLong(),   deskewedBottomLeft[2].toLong())
        val max = longArrayOf(img.max(0),deskewedBottomLeft[1].toLong(), deskewedTopRight[2].toLong())

        val realview: RealRandomAccessible<UnsignedShortType> = RealViews.affineReal(interpolated, affine)
//        val view: RandomAccessibleInterval<UnsignedShortType> = Views.interval(Views.raster(realview), img)
        val view: RandomAccessibleInterval<UnsignedShortType> = Views.interval(Views.raster(realview),
            min,max
//            longArrayOf(0,deskewedBottomCorner[1].toLong()+1,0),img.dimensionsAsLongArray()
        )


        volume = Volume.fromRAI(view, UnsignedShortType(), AxisOrder.DEFAULT, "T1 head", hub, VolumeViewerOptions())
        volume.transferFunction = TransferFunction.ramp(0.003f, 0.5f, 0.3f)
        volume.spatial(){
            //scale = Vector3f(0.1f,0.1f,0.5f,)
            scale = Vector3f(0.07f,0.07f,0.07f,)
            rotation = Quaternionf().rotateY((PI/2).toFloat()) // now z goes right
            //position = Vector3f(0f,-0.5f,0f)
        }
        volume.origin = Origin.FrontBottomLeft
        scene.addChild(volume)

        val bg = BoundingGrid()
        bg.node = volume
        volume.metadata["BoundingGrid"] = bg
        scene.addChild(bg)

        scene.addChild(Sphere(0.05f))


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

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            DeskewingExample().main()
        }
    }
}
