package microscenery.debug

import fromScenery.utils.extensions.times
import graphics.scenery.RichNode
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackedDevice
import graphics.scenery.controls.TrackedDeviceType
import graphics.scenery.controls.behaviours.PerButtonPressable
import graphics.scenery.controls.behaviours.Pressable
import graphics.scenery.controls.behaviours.SimplePressable
import graphics.scenery.numerics.Random
import microscenery.*
import microscenery.UI.UIModel
import microscenery.VRUI.MeasureTool
import microscenery.stageSpace.MicroscopeLayout
import microscenery.stageSpace.StageSpaceManager
import org.joml.Matrix4f
import org.joml.Vector3f
import org.scijava.ui.behaviour.DragBehaviour
import kotlin.concurrent.thread

/**
 * to use in desktop mode use:
 * https://www.reddit.com/r/Vive/comments/6uo053/how_to_use_steamvr_tracked_devices_without_a_hmd/
 */
class MeasureToolDebug : DefaultScene(VR = true, width = 1024, height = 1024) {
    val msHub = MicrosceneryHub(hub)

    init {
        MicroscenerySettings.set(Settings.StageSpace.ShowHullbox, false)
    }

    override fun init() {
        super.init()

        cam.spatial().position = Vector3f(0f, 0f, 1f)
        val root = RichNode()
        scene.addChild(root)


        val hw = DemoMicroscopeHardware(binning = 1)
        val stageSpaceManager = StageSpaceManager(
            hw,
            scene,
            msHub,
            layout = MicroscopeLayout.Default(MicroscopeLayout.Axis.Z)
        )

        val measureTool = MeasureTool(stageSpaceManager = stageSpaceManager, hmd = hmd, uiModel = msHub.getAttribute(UIModel::class.java))
        scene.addChild(measureTool)



        thread {
            Thread.sleep(3000)


            val range = 0.15f
            val square = listOf(
                Vector3f(-range, range, range),
                Vector3f(range, range, range),
                Vector3f(range, -range, range),
                Vector3f(-range, -range, range),
            )

            val rnd = (0..4).map { Random.random3DVectorFromRange(-0.5f, 0.5f) }

            rnd.forEach {
                measureTool.apply {
                    this.spatial().position = it
                    Thread.sleep(50)
                    //this.spatial().scale = Vector3f(0.3f)
                    (this.getAttributeOrNull(Pressable::class.java) as? PerButtonPressable)
                        ?.actions
                        ?.get(OpenVRHMD.OpenVRButton.Trigger)
                        ?.onRelease
                        ?.invoke(
                            this.spatial(),
                            TrackedDevice(TrackedDeviceType.Controller, "dummy", Matrix4f(), System.nanoTime())
                        )
                    Thread.sleep(50)
                }
            }

            val lNodes = scene.findByClassname("MeasurePoint").mapNotNull { it as? MeasureTool.MeasurePoint }

            while (true) {
                lNodes[2].spatial().position = Vector3f(100f) * ((System.currentTimeMillis() % 5000) / 5000f)
            }
        }
    }

    override fun inputSetup() {
        super.inputSetup()
        // debug: select 3dGUI menu entries with mouse
        inputHandler?.addBehaviour("debug3dClick", object : DragBehaviour {
            var pressable: SimplePressable? = null
            val controllerDevice =
                TrackedDevice(TrackedDeviceType.Generic, "dummy", Matrix4f().identity(), System.nanoTime())

            override fun init(p0: Int, p1: Int) {
                pressable = cam.getNodesForScreenSpacePosition(p0, p1).matches
                    .firstNotNullOfOrNull { it.node.getAttributeOrNull(Pressable::class.java) as? SimplePressable }
                pressable?.onPress?.invoke(cam.spatial(), controllerDevice)

            }

            override fun drag(p0: Int, p1: Int) {
                pressable?.onHold?.invoke(cam.spatial(), controllerDevice)
            }

            override fun end(p0: Int, p1: Int) {
                pressable?.onRelease?.invoke(cam.spatial(), controllerDevice)
                pressable = null
            }
        })
        inputHandler?.addKeyBinding("debug3dClick", "1")


    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            MeasureToolDebug().main()
        }
    }
}
