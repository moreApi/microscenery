package microscenery.example

import graphics.scenery.Origin
import graphics.scenery.volumes.TransferFunction
import graphics.scenery.volumes.Volume
import microscenery.DefaultVRScene
import microscenery.VRUI.VRUIManager
import org.joml.Vector3f

class VolRenVRCropping : DefaultVRScene(VolRenVRCropping::class.java.simpleName) {
    private lateinit var volume: Volume

    override fun init() {
        prepareVRScene()

        val head = Volume.VolumeFileSource(
            Volume.VolumeFileSource.VolumePath.Given("C:\\Users\\JanCasus\\volumes\\t1-head.tif"),
            Volume.VolumeFileSource.VolumeType.TIFF
        )
        volume = Volume.forNetwork(head, hub)
        volume.origin = Origin.FrontBottomLeft

        volume.transferFunction = TransferFunction.ramp(0.001f, 0.5f, 0.3f)
        volume.spatial {
            scale = Vector3f(5f)
        }
        scene.addChild(volume)

    }

    override fun inputSetup() {
        super.inputSetup()

        VRUIManager.initBehavior(scene, hmd, inputHandler, null) { volume }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            VolRenVRCropping().main()
        }
    }
}