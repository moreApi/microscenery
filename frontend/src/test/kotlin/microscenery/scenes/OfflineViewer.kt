package microscenery.scenes

import bdv.util.AxisOrder
import bvv.core.VolumeViewerOptions
import graphics.scenery.Hub
import graphics.scenery.Origin
import graphics.scenery.controls.behaviours.Switch
import graphics.scenery.utils.extensions.times
import graphics.scenery.volumes.Colormap
import graphics.scenery.volumes.TransferFunction
import graphics.scenery.volumes.TransferFunctionEditor
import graphics.scenery.volumes.Volume
import ij.IJ
import ij.ImagePlus
import microscenery.DefaultScene
import microscenery.DefaultVRScene
import microscenery.MicrosceneryHub
import microscenery.VRUI.VRUIManager
import microscenery.VRUI.fromScenery.WheelMenu
import net.imglib2.img.Img
import net.imglib2.img.display.imagej.ImageJFunctions
import net.imglib2.type.numeric.integer.UnsignedByteType
import net.imglib2.type.numeric.integer.UnsignedShortType
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.concurrent.thread
import kotlin.io.path.Path
import kotlin.math.PI

val openSpimScale3 = Vector3f(.225f, .225f, 3.348f)
val openSpimScale15 = Vector3f(.225f, .225f, 1.524f)

fun currentVolume(hub: Hub) = janasWingDisk(hub)

fun janasWingDisk(hub: Hub): Volume {
    val imp: ImagePlus = IJ.openImage("""E:\volumes\de-skewed\WingDisk_deskewed.tif""")
    val img: Img<UnsignedShortType> = ImageJFunctions.wrap(imp)

    val volume = Volume.fromRAI(
        img,
        UnsignedShortType(),
        AxisOrder.DEFAULT,
        "Volume loaded with IJ",
        hub,
        VolumeViewerOptions()
    )
    volume.spatial() {
        scale = Vector3f(5f,5f,8f)//openSpimScale15*2f
    }
    volume.origin = Origin.Center
    volume.transferFunction = TransferFunction.ramp(0.08605619f, 0.5125469f, 1f)
    volume.colormap = Colormap.get("plasma")
    volume.setTransferFunctionRange(1125.0f,3300.0f)

    return volume
}

fun spindle(hub: Hub): Volume {
    //val imp: ImagePlus = IJ.openImage("""E:\volumes\spindle\NikonSD_60x_HeLa_02.tif""")
    val imp: ImagePlus = IJ.openImage("""E:\volumes\spindle\NikonSD_100x_R1EmESC_01-1.tif""")
    val img: Img<UnsignedShortType> = ImageJFunctions.wrap(imp)

    val volume = Volume.fromRAI(
        img,
        UnsignedShortType(),
        AxisOrder.DEFAULT,
        "Volume loaded with IJ",
        hub,
        VolumeViewerOptions()
    )
    volume.spatial() {
        scale = Vector3f(5f,5f,8f)//openSpimScale15*2f
    }
    volume.origin = Origin.Center
    volume.transferFunction = TransferFunction.ramp(0.08605619f, 0.5125469f, 1f)
    volume.colormap = Colormap.get("plasma")
    volume.setTransferFunctionRange(1125.0f,3300.0f)

    thread {
        while (true){
            volume.spatial().rotation = Quaternionf().rotationY(2*PI.toFloat() * ((System.currentTimeMillis() % 10000) / 10000f))

        }
    }

    return volume
}


fun neuroStack4(hub: Hub): Volume {
    //val volume = Volume.fromXML("""C:\Users\JanCasus\volumes\embo\MariaPlant\export.xml""",hub, VolumeViewerOptions())
//    val imp: ImagePlus = IJ.openImage("""E:\volumes\Neuronenstacks\img_00115_unsigned_short.tif""")
    val imp: ImagePlus = IJ.openImage("""E:\volumes\Neuronenstacks\img_00118_unsigned_short.tif""")
//    val imp: ImagePlus = IJ.openImage("""E:\volumes\Neuronenstacks\img_00123_unsigned_short.tif""")
    val img: Img<UnsignedShortType> = ImageJFunctions.wrap(imp)

    val volume = Volume.fromRAI(
        img,
        UnsignedShortType(),
        AxisOrder.DEFAULT,
        "Volume loaded with IJ",
        hub,
        VolumeViewerOptions()
    )
    volume.spatial() {
        scale = openSpimScale15*2f
    }
    volume.origin = Origin.FrontBottomLeft
    volume.transferFunction = TransferFunction.ramp(0f, 0.8f, 1f)
    volume.colormap = Colormap.get("hot")
    volume.setTransferFunctionRange(100f, 1000f)

    return volume
}

fun lund(hub: Hub): Volume {
    //val volume = Volume.fromXML("""C:\Users\JanCasus\volumes\embo\MariaPlant\export.xml""",hub, VolumeViewerOptions())
    val imp: ImagePlus = IJ.openImage("""E:\volumes\embo\Lund-100MB.tif""")
    val img: Img<UnsignedByteType> = ImageJFunctions.wrap(imp)

    val volume = Volume.fromRAI(
        img,
        UnsignedByteType(),
        AxisOrder.DEFAULT,
        "Volume loaded with IJ",
        hub,
        VolumeViewerOptions()
    )
    volume.spatial() {
        scale = openSpimScale15
    }
    volume.origin = Origin.FrontBottomLeft
    volume.transferFunction = TransferFunction.ramp(0f, 0.8f, 1f)
    volume.colormap = Colormap.get("hot")
    volume.setTransferFunctionRange(400f, 2962f)

    return volume
}

fun hydra(hub: Hub): Volume {
    //val volume = Volume.fromXML("""C:\Users\JanCasus\volumes\embo\Drosphila_brain\export.xml""",hub, VolumeViewerOptions())
    val volume = Volume.fromXML("""E:\volumes\embo\Hydra\export.xml""", hub, VolumeViewerOptions())
    //val volume = Volume.fromPath(Path("""C:\Users\JanCasus\volumes\embo\Drosphila_brain\brain.tif"""),hub)
    volume.spatial() {
        scale = Vector3f(1.3934f)
    }
    volume.origin = Origin.FrontBottomLeft
    volume.transferFunction = TransferFunction.ramp(0f, 0.8f, 1f)
    volume.colormap = Colormap.get("hot")
    volume.setTransferFunctionRange(111f, 305f)

    var lastTimepoint = 0L
    volume.update += {
        val now = System.currentTimeMillis()
        if (now - lastTimepoint > 250) {
            if (volume.timepointCount - 1 <= volume.currentTimepoint)
                volume.goToFirstTimepoint()
            else
                volume.nextTimepoint()

            lastTimepoint = now
        }
    }

    return volume
}


fun cherry(hub: Hub): Volume {
    //val volume = Volume.fromXML("""C:\Users\JanCasus\volumes\embo\MariaPlant\export.xml""",hub, VolumeViewerOptions())
    val volume = Volume.fromPath(
        Path("""E:\volumes\unpublished\Pdu_H2BeGFP_CAAXmCherry_0123_20130312_192018.corrected-histone\SPC0_TM0008_CM0_CM1_CHN00_CHN01.fusedStack.tif"""),
        hub
    )
    volume.spatial() {
        scale = openSpimScale15
    }
    volume.origin = Origin.FrontBottomLeft
    volume.transferFunction = TransferFunction.ramp(0f, 0.8f, 1f)
    volume.colormap = Colormap.get("hot")
    volume.setTransferFunctionRange(400f, 2962f)

    return volume
}

fun ceratitis(hub: Hub): Volume {
    //val volume = Volume.fromXML("""C:\Users\JanCasus\volumes\embo\Ceratitis\export.xml""",hub, VolumeViewerOptions())
    val volume =
        Volume.fromXML("""e:\volumes\embo\Ceratitis\exportSmall.xml""", hub, VolumeViewerOptions())
    volume.spatial() {
        scale = Vector3f(1.9955f, 1.9955f * 0.5f * 0.66f, 0.9977f)
    }
    volume.origin = Origin.FrontBottomLeft
    volume.transferFunction = TransferFunction.ramp(0f, 0.8f, 1f)
    volume.colormap = Colormap.get("hot")
    volume.setTransferFunctionRange(27f, 2611f)

    val displayRangeStart = 500 to 2158
    val displayRangeEnd = 15 to 326
    val displayRangeDif =
        displayRangeStart.first - displayRangeEnd.first to displayRangeStart.second - displayRangeEnd.second
    var lastTimestamp = 0L
    volume.update += {
        val now = System.currentTimeMillis()
        if (now - lastTimestamp > 250) {
            if (volume.timepointCount - 1 <= volume.currentTimepoint)
                volume.goToFirstTimepoint()
            else
                volume.nextTimepoint()

            val percentage = volume.currentTimepoint.toFloat() / volume.timepointCount
            volume.setTransferFunctionRange(
                displayRangeEnd.first + displayRangeDif.first * (1 - percentage),
                displayRangeEnd.second + displayRangeDif.second * (1 - percentage)
            )

            lastTimestamp = now
        }
    }

    return volume
}

fun mariaPlant(hub: Hub): Volume {
    //val volume = Volume.fromXML("""C:\Users\JanCasus\volumes\embo\MariaPlant\export.xml""",hub, VolumeViewerOptions())
    val volume =
        Volume.fromPath(Path("""E:\volumes\embo\MariaPlant\plant_MMStack_Default.ome.tif"""), hub)
    volume.spatial() {
        scale = openSpimScale15
    }
    volume.origin = Origin.FrontBottomLeft
    volume.transferFunction = TransferFunction.ramp(0f, 0.8f, 1f)
    volume.colormap = Colormap.get("hot")
    volume.setTransferFunctionRange(400f, 2962f)

    return volume
}

fun drosphilaBrain(hub: Hub): Volume {
    //val volume = Volume.fromXML("""C:\Users\JanCasus\volumes\embo\Drosphila_brain\export.xml""",hub, VolumeViewerOptions())
    val volume = Volume.fromXML(
        """E:\volumes\embo\Drosphila_brain\exporBigBraint.xml""",
        hub,
        VolumeViewerOptions()
    )
    //val volume = Volume.fromPath(Path("""C:\Users\JanCasus\volumes\embo\Drosphila_brain\brain.tif"""),hub)
    volume.spatial() {
        scale = Vector3f(5.71699f, 5.716990f, 19.324049f ) * 0.1f
        rotation = Quaternionf().rotationX((Math.PI.toFloat() / 4) * 5.75f)
    }
    volume.origin = Origin.FrontBottomLeft
    volume.transferFunction = TransferFunction.ramp(0f, 0.8f, 1f)
    volume.colormap = Colormap.get("hot")
    volume.setTransferFunctionRange(321f, 1419f)

    return volume
}

fun mohammadMouseBrain(hub: Hub): Volume {
    val volume = Volume.fromXML(
        """E:\volumes\embo\mohammads_mouse_brain\export.xml""",
        hub,
        VolumeViewerOptions()
    )
    //val volume = Volume.fromPath(Path("""C:\Users\JanCasus\volumes\embo\MariaPlant\plant_MMStack_Default.ome.tif"""),hub)
    volume.spatial() {
        scale = Vector3f(1.3323542f, 1.3323542f, 4.992066f ) * 0.2f
    }
    volume.origin = Origin.FrontBottomLeft
    volume.transferFunction = TransferFunction.ramp(0f, 0.8f, 1f)
    volume.colormap = Colormap.get("jet")
    volume.setTransferFunctionRange(119f, 388f)

    return volume
}


class OfflineViewer2D : DefaultScene() {

    override fun init() {
        super.init()

        val vol = currentVolume(hub)
        scene.addChild(vol)
        TransferFunctionEditor.showTFFrame(vol)
        thread {
            while (true) {
                Thread.sleep(500)
                val s = vol
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            OfflineViewer2D().main()
        }
    }
}


class OfflineViewerVR() : DefaultVRScene("Embo Scene") {

    lateinit var vol: Volume

    override fun init() {
        super.init()

        thread {
            //delay volume loading to not crash VR...
            Thread.sleep(1000)
            vol = currentVolume(hub)
            scene.addChild(vol)
            TransferFunctionEditor.showTFFrame(vol)
        }

        thread {
            // debug loop
            while (true) {
                Thread.sleep(500)
                val s = vol
            }
        }
    }

    override fun inputSetup() {
        super.inputSetup()

        VRUIManager.initBehavior(
            scene, hmd, inputHandler, customActions =
            WheelMenu(hmd, listOf(Switch("freeze blocks",false){
                vol.volumeManager.freezeRequiredBlocks = it
            }), false,), msHub = MicrosceneryHub(hub)
        )
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            OfflineViewerVR().main()
        }
    }
}