package microscenery.scenes

import bdv.util.AxisOrder
import graphics.scenery.Box
import graphics.scenery.SceneryBase
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackerRole
import graphics.scenery.controls.behaviours.Grabable
import graphics.scenery.controls.behaviours.VRGrab
import graphics.scenery.volumes.SlicingPlane
import graphics.scenery.volumes.TransferFunction
import graphics.scenery.volumes.Volume
import ij.IJ
import ij.ImagePlus
import microscenery.DefaultVRScene
import microscenery.behaviors.VRTeleport
import net.imglib2.img.Img
import net.imglib2.img.display.imagej.ImageJFunctions
import net.imglib2.type.numeric.integer.UnsignedShortType
import org.joml.Vector3f
import org.scijava.ui.behaviour.ClickBehaviour
import tpietzsch.example2.VolumeViewerOptions


class VolRenVRCropping : DefaultVRScene(VolRenVRCropping::class.java.simpleName) {
    private lateinit var volume: Volume

    override fun init() {
        prepareVRScene()

        val imp: ImagePlus = IJ.openImage("https://imagej.nih.gov/ij/images/t1-head.zip")
        val img: Img<UnsignedShortType> = ImageJFunctions.wrapShort(imp)

        volume = Volume.fromRAI(img, UnsignedShortType(), AxisOrder.DEFAULT, "T1 head", hub, VolumeViewerOptions())
        volume.transferFunction = TransferFunction.ramp(0.001f, 0.5f, 0.3f)
        volume.spatial {
            scale = Vector3f(0.5f, -0.5f, 0.5f)
            position = Vector3f(0f, 1f, -1f)
        }
        scene.addChild(volume)

        val croppingHandle = Box(Vector3f(0.2f, 0.01f, 0.2f))
        croppingHandle.spatial {
            position = Vector3f(0f, 1f, -0.5f)
        }
        croppingHandle.addAttribute(Grabable::class.java, Grabable())
        scene.addChild(croppingHandle)

        val croppingPlane = SlicingPlane()
        croppingPlane.addTargetVolume(volume)
        volume.slicingMode = Volume.SlicingMode.Cropping
        croppingHandle.addChild(croppingPlane)
    }

    override fun inputSetup() {
        super.inputSetup()

        // We first grab the default movement actions from scenery's input handler,
        // and re-bind them on the right-hand controller's trackpad or joystick.
        inputHandler?.let { handler ->
            hashMapOf(
                "move_forward" to OpenVRHMD.keyBinding(TrackerRole.RightHand, OpenVRHMD.OpenVRButton.Up),
                "move_back" to OpenVRHMD.keyBinding(TrackerRole.RightHand, OpenVRHMD.OpenVRButton.Down),
                "move_left" to OpenVRHMD.keyBinding(TrackerRole.RightHand, OpenVRHMD.OpenVRButton.Left),
                "move_right" to OpenVRHMD.keyBinding(TrackerRole.RightHand, OpenVRHMD.OpenVRButton.Right)
            ).forEach { (name, key) ->
                handler.getBehaviour(name)?.let { b ->
                    logger.info("Adding behaviour $name bound to $key to HMD")
                    hmd.addBehaviour(name, b)
                    hmd.addKeyBinding(name, key)
                }
            }
        }
        // Finally, add a behaviour to toggle the scene's shell
        hmd.addBehaviour("toggle_shell", ClickBehaviour { _, _ ->
            hullbox.visible = !hullbox.visible
            logger.info("Hull visible: ${hullbox.visible}")
        })
        //... and bind that to the A button on the left-hand controller.
        //hmd.addKeyBinding("toggle_shell", TrackerRole.LeftHand, OpenVRHMD.OpenVRButton.A)

        // slicing mode toggle
        hmd.addBehaviour("toggleSlicing", ClickBehaviour { _, _ ->
            val current = volume.slicingMode.id
            val next = (current + 1) % Volume.SlicingMode.values().size
            volume.slicingMode = Volume.SlicingMode.values()[next]
        })
        hmd.addKeyBinding("toggleSlicing", TrackerRole.RightHand, OpenVRHMD.OpenVRButton.A)

        VRGrab.createAndSet(scene, hmd, listOf(OpenVRHMD.OpenVRButton.Side), listOf(TrackerRole.RightHand))

        VRTeleport.createAndSet(scene,hmd, listOf(OpenVRHMD.OpenVRButton.A) ,listOf(TrackerRole.LeftHand))
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            VolRenVRCropping().main()
        }
    }
}
