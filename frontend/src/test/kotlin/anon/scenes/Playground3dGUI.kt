package anon.scenes

import fromScenery.utils.extensions.times
import graphics.scenery.RichNode
import graphics.scenery.controls.TrackedDevice
import graphics.scenery.controls.TrackedDeviceType
import graphics.scenery.controls.behaviours.Pressable
import graphics.scenery.controls.behaviours.SimplePressable
import graphics.scenery.numerics.Random
import anon.DefaultScene
import anon.MicroscenerySettings
import anon.Settings
import anon.VRUI.Gui3D.*
import anon.primitives.LineNode
import org.joml.Matrix4f
import org.joml.Vector3f
import org.scijava.ui.behaviour.DragBehaviour
import kotlin.concurrent.thread
import kotlin.math.PI

/**
 * https://www.reddit.com/r/Vive/comments/6uo053/how_to_use_steamvr_tracked_devices_without_a_hmd/
 */
class Playground3dGUI() : DefaultScene(VR = false, width = 1024, height = 1024) {
    init {
        MicroscenerySettings.set(Settings.StageSpace.ShowHullbox,true)
    }
    override fun init() {
        super.init()

        cam.spatial().position = Vector3f(0f, 0f, 10f)

        val menu = Column(
            Row(TextBox("laser power", height = 0.8f)),
            ValueEdit.Companion.forFloatSetting(Settings.Ablation.LaserPower, 0.1f),
            Row(TextBox("step size", height = 0.8f)),
            ValueEdit.Companion.forIntSetting(Settings.Ablation.StepSizeUm, 10),
            Row(TextBox("repetitions", height = 0.8f)),
            ValueEdit.Companion.forIntSetting(Settings.Ablation.Repetitions, plusPlusButtons = false),
            Row(Button("ablate", height = 1.3f) {
            })
        )
        //scene.addChild(menu)

        val root = RichNode()
        //root.spatial().scale = Vector3f(0.3f)
        scene.addChild(root)

        val lNodes = (0..4).map { LineNode().apply {
            root.addChild(this)
            this.spatial().position = Random.Companion.random3DVectorFromRange(-3.0f,3.0f)
            this.spatial().scale = Vector3f(0.3f)
            this.material().diffuse = Random.Companion.random3DVectorFromRange(0f,1f)
            this.lineMaterial = this.material()
        } }
        lNodes.forEachIndexed { index, lineNode -> if (index > 0) lineNode.connectTo(lNodes[index - 1])}
        lNodes[0].connectTo(lNodes[4])

        thread {
            while (true) {
                lNodes[2].spatial().position = Vector3f(3f) * ((System.currentTimeMillis() % 5000) / 5000f)
                root.rotation.rotationY(PI.toFloat() * 2f * ((System.currentTimeMillis() % 5000) / 5000f))
                root.spatial().needsUpdate = true
            }
        }

        thread {
            lNodes
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
                pressable = cam.getNodesForScreenSpacePosition(p0,p1).matches
                    .firstNotNullOfOrNull { it.node.getAttributeOrNull(Pressable::class.java) as? SimplePressable }
                pressable?.onPress?.invoke(cam.spatial(),controllerDevice)

            }
            override fun drag(p0: Int, p1: Int) {
                pressable?.onHold?.invoke(cam.spatial(),controllerDevice)
            }

            override fun end(p0: Int, p1: Int) {
                pressable?.onRelease?.invoke(cam.spatial(), controllerDevice)
                pressable = null
            }
        })
        inputHandler?.addKeyBinding("debug3dClick","1")


    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Playground3dGUI().main()
        }
    }
}