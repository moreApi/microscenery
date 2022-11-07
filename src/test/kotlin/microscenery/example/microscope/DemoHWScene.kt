package microscenery.example.microscope

import graphics.scenery.BoundingGrid
import graphics.scenery.Box
import graphics.scenery.attribute.material.Material
import graphics.scenery.ui.SwingBridgeFrame
import graphics.scenery.ui.SwingUiNode
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import graphics.scenery.volumes.TransferFunctionEditor
import microscenery.DefaultScene
import microscenery.StageSpaceManager
import microscenery.hardware.DemoMicroscopeHardware
import org.joml.Vector3f
import org.scijava.ui.behaviour.ClickBehaviour
import org.scijava.ui.behaviour.DragBehaviour
import kotlin.concurrent.thread

class DemoHWScene : DefaultScene() {
    init {
        logger.info("Starting demo hw scene")

        val hw = DemoMicroscopeHardware()
        val stageSpaceManager = StageSpaceManager(hw, scene, hub, addFocusFrame = true)

        stageSpaceManager.stageRoot.spatial().scale *= Vector3f(1f, 1f, 2f)

        val bridge = SwingBridgeFrame("1DTransferFunctionEditor")
        val tfUI = TransferFunctionEditor(650, 550, stageSpaceManager, bridge)
        tfUI.name = "Slices"
        val swingUiNode = tfUI.mainFrame.uiNode
        swingUiNode.spatial() {
            position = Vector3f(2f,0f,0f)
        }

        scene.addChild(swingUiNode)

        val hullbox = Box(Vector3f(20.0f, 20.0f, 20.0f), insideNormals = true)
        hullbox.name = "hullbox"
        hullbox.material {
            ambient = Vector3f(0.6f, 0.6f, 0.6f)
            diffuse = Vector3f(0.4f, 0.4f, 0.4f)
            specular = Vector3f(0.0f, 0.0f, 0.0f)
            cullingMode = Material.CullingMode.Front
        }
        scene.addChild(hullbox)

        thread {
            //Thread.sleep(5000)
            val db = DemoBehavior(
                hw.side.toFloat(),
                stageSpaceManager
            )
            db.fixedStack()
            Thread.sleep(2500)
            db.fixed()
        }
        thread {
            while (true) {
                Thread.sleep(200)
                scene to stageSpaceManager
            }
        }
    }

    /**
     * Adds InputBehaviour -> MouseClick, Drag and Ctrl-Click to interact with the SwingUI using a Scenery Plane (SwingUINode)
     */
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

