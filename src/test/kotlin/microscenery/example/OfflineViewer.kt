package microscenery.example

import graphics.scenery.*
import graphics.scenery.utils.extensions.times
import graphics.scenery.volumes.Colormap
import graphics.scenery.volumes.TransferFunction
import graphics.scenery.volumes.Volume
import microscenery.DefaultScene
import microscenery.DefaultVRScene
import microscenery.UI.DisplayRangeEditor
import microscenery.VRUI.VRUIManager
import org.joml.Quaternionf
import org.joml.Vector3f
import tpietzsch.example2.VolumeViewerOptions
import kotlin.concurrent.thread
import kotlin.io.path.Path

val openSpimScale3 = Vector3f(.225f,.225f,3.348f)
val openSpimScale15 = Vector3f(.225f,.225f,1.524f)

fun currentVolume(hub:Hub) = cherry(hub)


fun hydra(hub:Hub):Volume {
    //val volume = Volume.fromXML("""C:\Users\JanCasus\volumes\embo\Drosphila_brain\export.xml""",hub, VolumeViewerOptions())
    val volume = Volume.fromXML("""C:\Users\JanCasus\volumes\embo\Hydra\export.xml""",hub, VolumeViewerOptions())
    //val volume = Volume.fromPath(Path("""C:\Users\JanCasus\volumes\embo\Drosphila_brain\brain.tif"""),hub)
    volume.spatial() {
        scale = Vector3f(1.3934f)*0.5f
    }
    volume.origin = Origin.FrontBottomLeft
    volume.transferFunction = TransferFunction.ramp(0f,0.8f,1f)
    volume.colormap = Colormap.get("hot")
    volume.setTransferFunctionRange(111f, 305f)

    var lastTimepoint = 0L
    volume.update += {
        val now = System.currentTimeMillis()
        if (now - lastTimepoint > 250){
            if (volume.timepointCount-1 <= volume.currentTimepoint)
                volume.goToFirstTimepoint()
            else
                volume.nextTimepoint()

            lastTimepoint = now
        }
    }

    return volume
}


fun cherry(hub:Hub):Volume {
    //val volume = Volume.fromXML("""C:\Users\JanCasus\volumes\embo\MariaPlant\export.xml""",hub, VolumeViewerOptions())
    val volume = Volume.fromPath(Path("""C:\Users\JanCasus\volumes\unpublished\Pdu_H2BeGFP_CAAXmCherry_0123_20130312_192018.corrected-histone\SPC0_TM0008_CM0_CM1_CHN00_CHN01.fusedStack.tif"""),hub)
    volume.spatial() {
        scale = openSpimScale15
    }
    volume.origin = Origin.FrontBottomLeft
    volume.transferFunction = TransferFunction.ramp(0f,0.8f,1f)
    volume.colormap = Colormap.get("hot")
    volume.setTransferFunctionRange(400f, 2962f)

    return volume
}

fun ceratitis(hub:Hub):Volume {
    //val volume = Volume.fromXML("""C:\Users\JanCasus\volumes\embo\Ceratitis\export.xml""",hub, VolumeViewerOptions())
    val volume = Volume.fromXML("""C:\Users\JanCasus\volumes\embo\Ceratitis\exportSmall.xml""",hub, VolumeViewerOptions())
    volume.spatial() {
        scale = Vector3f(1.9955f, 1.9955f*0.5f*0.66f,0.9977f)*0.1f
    }
    volume.origin = Origin.FrontBottomLeft
    volume.transferFunction = TransferFunction.ramp(0f,0.8f,1f)
    volume.colormap = Colormap.get("hot")
    volume.setTransferFunctionRange(27f, 2611f)

    val displayRangeStart = 500 to 2158
    val displayRangeEnd = 15 to 326
    val displayRangeDif = displayRangeStart.first - displayRangeEnd.first to displayRangeStart.second - displayRangeEnd.second
    var lastTimestamp = 0L
    volume.update += {
        val now = System.currentTimeMillis()
        if (now - lastTimestamp > 250){
            if (volume.timepointCount-1 <= volume.currentTimepoint)
                volume.goToFirstTimepoint()
            else
                volume.nextTimepoint()

            val percentage = volume.currentTimepoint.toFloat() / volume.timepointCount
            volume.setTransferFunctionRange(displayRangeEnd.first + displayRangeDif.first * (1-percentage), displayRangeEnd.second + displayRangeDif.second * (1-percentage))

            lastTimestamp = now
        }
    }

    return volume
}

fun mariaPlant(hub:Hub):Volume {
    //val volume = Volume.fromXML("""C:\Users\JanCasus\volumes\embo\MariaPlant\export.xml""",hub, VolumeViewerOptions())
    val volume = Volume.fromPath(Path("""C:\Users\JanCasus\volumes\embo\MariaPlant\plant_MMStack_Default.ome.tif"""),hub)
    volume.spatial() {
        scale = openSpimScale15
    }
    volume.origin = Origin.FrontBottomLeft
    volume.transferFunction = TransferFunction.ramp(0f,0.8f,1f)
    volume.colormap = Colormap.get("hot")
    volume.setTransferFunctionRange(400f, 2962f)

    return volume
}

fun drosphilaBrain(hub:Hub):Volume {
    //val volume = Volume.fromXML("""C:\Users\JanCasus\volumes\embo\Drosphila_brain\export.xml""",hub, VolumeViewerOptions())
    val volume = Volume.fromXML("""C:\Users\JanCasus\volumes\embo\Drosphila_brain\exporBigBraint.xml""",hub, VolumeViewerOptions())
    //val volume = Volume.fromPath(Path("""C:\Users\JanCasus\volumes\embo\Drosphila_brain\brain.tif"""),hub)
    volume.spatial() {
        scale = Vector3f(5.71699f, 5.716990f, 19.324049f*2)*0.1f
        rotation = Quaternionf().rotationX((Math.PI.toFloat()/4)*5.75f)
    }
    volume.origin = Origin.FrontBottomLeft
    volume.transferFunction = TransferFunction.ramp(0f,0.8f,1f)
    volume.colormap = Colormap.get("hot")
    volume.setTransferFunctionRange(321f, 1419f)

    return volume
}

fun mohammadMouseBrain(hub:Hub):Volume {
    val volume = Volume.fromXML("""C:\Users\JanCasus\volumes\embo\mohammads_mouse_brain\export.xml""",hub, VolumeViewerOptions())
    //val volume = Volume.fromPath(Path("""C:\Users\JanCasus\volumes\embo\MariaPlant\plant_MMStack_Default.ome.tif"""),hub)
    volume.spatial() {
        scale = Vector3f(1.3323542f, 1.3323542f, 4.992066f*5) * 0.02f
    }
    volume.origin = Origin.FrontBottomLeft
    volume.transferFunction = TransferFunction.ramp(0f,0.8f,1f)
    volume.colormap = Colormap.get("jet")
    volume.setTransferFunctionRange(119f, 388f)

    return volume
}


class OfflineViewer2D : DefaultScene() {

    override fun init() {
        super.init()

        val vol = currentVolume(hub)
        scene.addChild(vol)
        DisplayRangeEditor(vol.converterSetups.first())
        thread {
            while (true){
                Thread.sleep(500)
                val s = vol
            }
        }
    }

    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            OfflineViewer2D().main()
        }
    }
}


class OfflineViewerVR() : DefaultVRScene("Embo Scene") {

    override fun init() {
        super.init()

        val vol = currentVolume(hub)
        scene.addChild(vol)

        DisplayRangeEditor(vol.converterSetups.first())

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
            OfflineViewerVR().main()
        }
    }
}