package microscenery.example

import graphics.scenery.controls.TrackedDevice
import graphics.scenery.controls.TrackedDeviceType
import graphics.scenery.controls.behaviours.PerButtonPressable
import graphics.scenery.controls.behaviours.Pressable
import graphics.scenery.controls.behaviours.SimplePressable
import microscenery.DefaultScene
import microscenery.Settings
import microscenery.VRUI.Gui3D.*
import org.joml.Matrix4f
import org.joml.Vector3f
import org.scijava.ui.behaviour.DragBehaviour
import kotlin.concurrent.thread

class Playground() : DefaultScene() {
    override fun init() {
        super.init()

        cam.spatial().position = Vector3f(0f,0f,10f)

        val menu = Column(
            Row(TextBox("laser power", height = 0.8f)),
            ValueEdit.forFloatSetting(Settings.Ablation.LaserPower,0.1f),
            Row(TextBox("step size", height = 0.8f)),
            ValueEdit.forIntSetting(Settings.Ablation.StepSizeUm,10),
            Row(TextBox("repetitions", height = 0.8f)),
            ValueEdit.forIntSetting(Settings.Ablation.Repetitions, plusPlusButtons = false),
            Row(Button("ablate", height = 1.3f){
            })
        )
        scene.addChild(menu)

        thread {
            menu
        }
    }

    override fun inputSetup() {
        super.inputSetup()
        inputHandler?.addBehaviour("debug3dClick", object : DragBehaviour {
            var pressable: SimplePressable? = null
            val controllerDevice = TrackedDevice(TrackedDeviceType.Generic,"dummy", Matrix4f().identity(),System.nanoTime())

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
            Playground().main()
        }
    }
}
