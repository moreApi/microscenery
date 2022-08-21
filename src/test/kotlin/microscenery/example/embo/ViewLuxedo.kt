package microscenery.example.embo

import graphics.scenery.BoundingGrid
import graphics.scenery.Box
import graphics.scenery.Origin
import graphics.scenery.utils.extensions.timesAssign
import graphics.scenery.volumes.Colormap
import graphics.scenery.volumes.TransferFunction
import graphics.scenery.volumes.Volume
import microscenery.DefaultScene
import microscenery.DefaultVRScene
import microscenery.VRUI.VRUIManager
import net.imagej.app.ImageJApp
import net.imglib2.type.numeric.integer.UnsignedShortType
import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil
import tpietzsch.example2.VolumeViewerOptions
import kotlin.concurrent.thread
import ij.IJ
import org.janelia.saalfeldlab.n5.N5FSReader
import org.janelia.saalfeldlab.n5.N5FSWriter
import kotlin.io.path.Path

class ViewLuxedo : DefaultScene() {
//class ViewLuxedo : DefaultVRScene("Embo Scene") {

    override fun init() {
        super.init()

        // Required so the volumeManager is initialized later in the scene -.-
        val dummyVolume = Volume.fromBuffer(emptyList(), 5, 5, 5, UnsignedShortType(), hub)
        dummyVolume.spatial().position = Vector3f(999f)
        dummyVolume.name = "dummy volume"
        dummyVolume.addTimepoint("bums", MemoryUtil.memAlloc(5 * 5 * 5 * Short.SIZE_BYTES))
        scene.addChild(dummyVolume)

        thread {
            Thread.sleep(5000)
            println("ok letz go")
            //val volume = Volume.fromPath(Path("""C:\Users\JanCasus\volumes\MuVi_embryoid_40X\MuVi_embryoid_40X.tif"""),hub)
            //val volume = Volume.fromXML("""C:\Users\JanCasus\volumes\drosophila.xml""",hub, VolumeViewerOptions())
            //val volume = Volume.fromXML("""D:\Pre_scan\2022-08-18_095843\bdv.xml""",hub, VolumeViewerOptions())
            //val volume = Volume.fromXML("""C:\Users\JanCasus\volumes\embo\someLuxendoThing\bdvexport.xml""",hub, VolumeViewerOptions())
            val volume = Volume.fromXML("""C:\Users\JanCasus\volumes\embo\someLuxendoThing\embryoid_40x.xml""",hub, VolumeViewerOptions())
            volume.spatial() {
                scale.z = 4f
                scale *= 0.1f
            }
            volume.origin = Origin.FrontBottomLeft
            volume.transferFunction = TransferFunction.ramp(0.003f,1f,0.005f)
            volume.colormap = Colormap.get("plasma")
            //volume.transferFunction = TransferFunction.flat(1f)//ramp(0.00f,01.0f,0.000f)
            scene.addChild(volume)
            BoundingGrid().node = volume
        }


        scene.addChild(Box(Vector3f(0.1f)))

        thread {
            while (true){
                Thread.sleep(500)
                val s = scene
            }
        }
    }

    override fun inputSetup() {
        super.inputSetup()

        //VRUIManager.initBehavior(scene, hmd, inputHandler) {scene.findByClassname(Volume::class.simpleName!!).first() as Volume}
    }

    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            ViewLuxedo().main()
        }
    }
}
/*
class ViewLuxedoImport : DefaultScene(name = "Embo Scene") {

    override fun init() {
        super.init()

        // Required so the volumeManager is initialized later in the scene -.-
        val dummyVolume = Volume.fromBuffer(emptyList(), 5, 5, 5, UnsignedShortType(), hub)
        dummyVolume.spatial().position = Vector3f(999f)
        dummyVolume.name = "dummy volume"
        dummyVolume.addTimepoint("bums", MemoryUtil.memAlloc(5 * 5 * 5 * Short.SIZE_BYTES))
        //scene.addChild(dummyVolume)


        val volume = Volume.fromPath(Path("""C:\Users\JanCasus\volumes\t1-head.tif"""),hub)
        scene.addChild(volume)

        thread{
            val file = """C:\Users\JanCasus\volumes\embo\someLuxendoThing\Cam_Long_00000.lux.h5"""
            ///IJ.run("Scriptable load HDF5...", "load="+file+" datasetnames=/Data nframes=1 nchannels=1")


            //Thread.sleep(4000)
            println("ok letz go")
            //val volume = Volume.fromPath(Path("""C:\Users\JanCasus\volumes\MuVi_embryoid_40X\MuVi_embryoid_40X.tif"""),hub)
            //val volume = Volume.fromXML("""C:\Users\JanCasus\volumes\drosophila.xml""",hub, VolumeViewerOptions())
            //val volume = Volume.fromXML("""D:\Pre_scan\2022-08-18_095843\bdv.xml""",hub, VolumeViewerOptions())
            //val volume = Volume.fromXML("""C:\Users\JanCasus\volumes\embo\someLuxendoThing\bdvexport.xml""",hub, VolumeViewerOptions())
            val volume = Volume.fromXML(targetPath,hub, VolumeViewerOptions())
            volume.spatial() {
                scale.z = 4f
                scale *= 0.1f
            }
            volume.origin = Origin.FrontBottomLeft
            volume.transferFunction = TransferFunction.ramp(0.003f,1f,0.005f)
            volume.colormap = Colormap.get("plasma")
            //volume.transferFunction = TransferFunction.flat(1f)//ramp(0.00f,01.0f,0.000f)
            scene.addChild(volume)
            BoundingGrid().node = volume
        }

        scene.addChild(Box(Vector3f(0.1f)))

        thread {
            while (true){
                Thread.sleep(500)
                val s = scene
            }
        }
    }

    override fun inputSetup() {
        super.inputSetup()

        //VRUIManager.initBehavior(scene, hmd, inputHandler) {scene.findByClassname(Volume::class.simpleName!!).first() as Volume}
    }

    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            ViewLuxedoImport().main()
        }
    }
}*/
//class ViewRadiolarien : DefaultScene() {
class ViewRadiolarien : DefaultVRScene("Embo Scene") {

    override fun init() {
        super.init()

        // Required so the volumeManager is initialized later in the scene -.-
        val dummyVolume = Volume.fromBuffer(emptyList(), 5, 5, 5, UnsignedShortType(), hub)
        dummyVolume.spatial().position = Vector3f(999f)
        dummyVolume.name = "dummy volume"
        dummyVolume.addTimepoint("bums", MemoryUtil.memAlloc(5 * 5 * 5 * Short.SIZE_BYTES))
        scene.addChild(dummyVolume)

        thread {
            Thread.sleep(5000)
            println("ok letz go")
            //val volume = Volume.fromPath(Path("""C:\Users\JanCasus\volumes\MuVi_embryoid_40X\MuVi_embryoid_40X.tif"""),hub)
            //val volume = Volume.fromXML("""C:\Users\JanCasus\volumes\drosophila.xml""",hub, VolumeViewerOptions())
            //val volume = Volume.fromXML("""D:\Pre_scan\2022-08-18_095843\bdv.xml""",hub, VolumeViewerOptions())
            val volume = Volume.fromXML("""C:\Users\JanCasus\volumes\embo\Radiolarien\Radiolarien-No1_MMStack_Pos0.ome.deskewed.xml""",hub, VolumeViewerOptions())
            volume.spatial() {
                scale.z = 4f
                scale *= 0.1f
            }
            volume.origin = Origin.FrontBottomLeft
            volume.transferFunction = TransferFunction.ramp(0.605f,0.9f,0.30f)
            volume.colormap = Colormap.get("plasma")
            //volume.transferFunction = TransferFunction.flat(1f)//ramp(0.00f,01.0f,0.000f)
            scene.addChild(volume)
            BoundingGrid().node = volume

            scene.removeChild(dummyVolume)
        }


        scene.addChild(Box(Vector3f(0.1f)))

        thread {
            while (true){
                Thread.sleep(500)
                val s = scene
            }
        }
    }

    override fun inputSetup() {
        super.inputSetup()

        VRUIManager.initBehavior(scene, hmd, inputHandler) {scene.findByClassname(Volume::class.simpleName!!).first() as Volume}
    }

    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            ViewRadiolarien().main()
        }
    }
}