package microscenery.example

import graphics.scenery.BoundingGrid
import graphics.scenery.Box
import graphics.scenery.attribute.material.Material
import graphics.scenery.numerics.Random
import graphics.scenery.ui.SwingBridgeFrame
import graphics.scenery.ui.SwingUiNode
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import graphics.scenery.volumes.TransferFunctionEditor
import microscenery.DefaultScene
import microscenery.StageSpaceManager
import microscenery.hardware.DemoMicroscopeHardware
import microscenery.nowMillis
import org.joml.Vector3f
import org.scijava.ui.behaviour.ClickBehaviour
import org.scijava.ui.behaviour.DragBehaviour
import kotlin.concurrent.thread

private enum class Mode {
    RandomStatic,
    Fixed,
    RandomLive
}

class DemoHWScene : DefaultScene() {

    init {

        val hw = DemoMicroscopeHardware()
        val stageSpaceManager = StageSpaceManager(hw, scene, hub, addFocusFrame = true)

        stageSpaceManager.stageRoot.spatial().scale *= Vector3f(1f, 1f, 2f)

        val hullbox = Box(Vector3f(20.0f, 20.0f, 20.0f), insideNormals = true)
        hullbox.name = "hullbox"
        hullbox.material {
            ambient = Vector3f(0.6f, 0.6f, 0.6f)
            diffuse = Vector3f(0.4f, 0.4f, 0.4f)
            specular = Vector3f(0.0f, 0.0f, 0.0f)
            cullingMode = Material.CullingMode.Front
        }
        scene.addChild(hullbox)


        val bridge = SwingBridgeFrame("1DTransferFunctionSliceEditor")
        val tfUI = TransferFunctionEditor(650, 550, stageSpaceManager, bridge)
        tfUI.name = "Slices"
        val swingUiNode = tfUI.mainFrame.uiNode
        swingUiNode.spatial() {
            position = Vector3f(2f,0f,0f)
        }
        scene.addChild(swingUiNode)


        when (Mode.RandomStatic) {
            Mode.RandomStatic -> {
                for (i in 0..1) {
                    val target = Random.random3DVectorFromRange(0f, hw.side.toFloat())
                    stageSpaceManager.stagePosition = target
                    stageSpaceManager.snapSlice()
                }
            }
            Mode.Fixed -> {
                for (z in listOf(0, 50, 100, 150, 199))
                    for (y in listOf(0, 50, 100, 150))
                        for (x in listOf(0, 50, 100, 150)) {
                            val target = Vector3f(x.toFloat(), y.toFloat(), z.toFloat())
                            stageSpaceManager.stagePosition = target
                            stageSpaceManager.snapSlice()
                        }
            }
            Mode.RandomLive -> {
                //stageSpaceManager.live(true)
                stageSpaceManager.focusFrame?.let { focus ->
                    var start = Vector3f()
                    var target = Vector3f()
                    var startTime = 0L
                    val travelTime = 3000
                    focus.update += {
                        focus.spatial {

                            if (nowMillis() > startTime + travelTime) {
                                startTime = nowMillis()
                                position = target
                                start = position
                                target = Random.random3DVectorFromRange(0f, hw.side.toFloat())
                            }

                            val dir = target - start
                            val relPos = (nowMillis() - startTime) / travelTime.toFloat()
                            position = start + (dir * relPos)
                            //stageSpaceManager.stagePosition = position
                            //stageSpaceManager.snapSlice()
                        }
                    }
                }
            }
        }

        thread {
            while (true) {
                Thread.sleep(200)
                scene to stageSpaceManager
            }
        }
    }

    override fun inputSetup() {
        super.inputSetup()

        val debugRaycast = false
        inputHandler?.addBehaviour(
            "ctrlClickObject", object : ClickBehaviour {
                override fun click(x: Int, y: Int) {
                    val ray = cam.getNodesForScreenSpacePosition(x,y, listOf<Class<*>>(BoundingGrid::class.java), debugRaycast)

                    ray.matches.firstOrNull()?.let { hit ->
                        val node = hit.node as? SwingUiNode ?: return
                        val hitPos = ray.initialPosition + ray.initialDirection * hit.distance
                        node.ctrlClick(hitPos)
                    }
                }
            }
        )
        inputHandler?.addBehaviour(
            "dragObject", object : DragBehaviour {
                override fun init(x:Int, y: Int) {
                    val ray = cam.getNodesForScreenSpacePosition(x,y, listOf<Class<*>>(BoundingGrid::class.java), debugRaycast)

                    ray.matches.firstOrNull()?.let { hit ->
                        val node = hit.node as? SwingUiNode ?: return
                        val hitPos = ray.initialPosition + ray.initialDirection * hit.distance
                        node.pressed(hitPos)
                    }
                }
                override fun drag(x: Int, y: Int) {
                    val ray = cam.getNodesForScreenSpacePosition(x,y, listOf<Class<*>>(BoundingGrid::class.java), debugRaycast)

                    ray.matches.firstOrNull()?.let { hit ->
                        val node = hit.node as? SwingUiNode ?: return
                        val hitPos = ray.initialPosition + ray.initialDirection * hit.distance
                        node.drag(hitPos)
                    }
                }
                override fun end(x: Int, y: Int) {
                    val ray = cam.getNodesForScreenSpacePosition(x,y, listOf<Class<*>>(BoundingGrid::class.java), debugRaycast)

                    ray.matches.firstOrNull()?.let { hit ->
                        val node = hit.node as? SwingUiNode ?: return
                        val hitPos = ray.initialPosition + ray.initialDirection * hit.distance
                        node.released(hitPos)
                    }
                }
            }
        )
        inputHandler?.addKeyBinding("dragObject", "1")
        inputHandler?.addKeyBinding("ctrlClickObject", "2")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            DemoHWScene().main()
        }
    }

}