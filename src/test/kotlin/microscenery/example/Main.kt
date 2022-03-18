package microscenery.example

import graphics.scenery.Box
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackerRole
import graphics.scenery.controls.behaviours.Grabable
import graphics.scenery.controls.behaviours.VRScale
import graphics.scenery.controls.behaviours.VRSelect
import graphics.scenery.controls.behaviours.VRSelectionWheel
import graphics.scenery.utils.Wiggler
import graphics.scenery.utils.extensions.times
import graphics.scenery.volumes.SlicingPlane
import graphics.scenery.volumes.Volume
import microscenery.DefaultVRScene
import microscenery.MMConnection
import microscenery.StreamedVolume
import microscenery.TransferFunction1DEditor
import microscenery.behaviors.VRGrabWithSelfMove
import microscenery.behaviors.VRTeleport
import org.joml.Vector3f
import kotlin.concurrent.thread

class Main : DefaultVRScene(Main::class.java.simpleName) {
    private lateinit var volume: Volume

    override fun init() {
        prepareVRScene()

        val slices = 112
        val mmConnection = MMConnection(slices)
        val mmConnectionVolume = StreamedVolume(
            hub,
            mmConnection.width,
            mmConnection.height,
            slices
        ) { mmConnection.captureStack(it.asShortBuffer()) }
        volume = mmConnectionVolume.volume
        scene.addChild(volume)

        val transEdit = TransferFunction1DEditor()
        scene.addChild(transEdit)

        volume.transferFunction = transEdit.transferFunction
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

        VRGrabWithSelfMove.createAndSet(
            scene,
            hmd,
            listOf(OpenVRHMD.OpenVRButton.Side),
            listOf(TrackerRole.LeftHand, TrackerRole.RightHand)
        )

        VRScale.createAndSet(hmd, OpenVRHMD.OpenVRButton.Side) {
            volume.ifSpatial { scale *= Vector3f(it) }
        }

        VRSelect.createAndSet(scene,
            hmd,
            listOf(OpenVRHMD.OpenVRButton.Trigger),
            listOf(TrackerRole.LeftHand),
            { n ->
                // this is just some action to show a successful selection.
                // Party Cube!
                val w = Wiggler(n.spatialOrNull()!!, 1.0f)
                thread {
                    Thread.sleep(2 * 1000)
                    w.deativate()
                }
            })

        VRTeleport.createAndSet(scene, hmd, listOf(OpenVRHMD.OpenVRButton.A), listOf(TrackerRole.RightHand))

        var tmpWheel: VRSelectionWheel? = null
        val future = VRSelectionWheel.createAndSet(scene, hmd,
            listOf(OpenVRHMD.OpenVRButton.A), listOf(TrackerRole.LeftHand),
            listOf(
                "Toggle Shell" to {
                    hullbox.visible = !hullbox.visible
                    logger.info("Hull visible: ${hullbox.visible}")
                },
                "Slicing Plane" to {
                    val croppingHandle = Box(Vector3f(0.2f, 0.01f, 0.2f))
                    croppingHandle.spatial {
                        position = Vector3f(0f, 1f, -0.5f)
                    }
                    croppingHandle.addAttribute(Grabable::class.java, Grabable())
                    tmpWheel?.let { croppingHandle.spatial().position = it.controller.worldPosition() }
                    scene.addChild(croppingHandle)

                    val croppingPlane = SlicingPlane()
                    croppingPlane.addTargetVolume(volume)
                    volume.slicingMode = Volume.SlicingMode.Cropping
                    croppingHandle.addChild(croppingPlane)
                },
                "Slicing Mode" to {
                    val current = volume.slicingMode.id
                    val next = (current + 1) % Volume.SlicingMode.values().size
                    volume.slicingMode = Volume.SlicingMode.values()[next]
                }
            ))
        thread { tmpWheel = future.get() }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Main().main()
        }
    }
}