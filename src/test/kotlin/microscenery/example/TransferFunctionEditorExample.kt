package microscenery.example

import graphics.scenery.*
import graphics.scenery.backends.Renderer
import graphics.scenery.ui.SwingBridgeFrame
import graphics.scenery.ui.SwingUiNode
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import graphics.scenery.volumes.Colormap
import graphics.scenery.volumes.TransferFunctionEditor
import graphics.scenery.volumes.Volume
import microscenery.DefaultScene
import org.joml.Vector3f
import org.scijava.ui.behaviour.ClickBehaviour
import org.scijava.ui.behaviour.DragBehaviour
import tpietzsch.example2.VolumeViewerOptions


/**
 * @author Konrad Michel <Konrad.Michel@mailbox.tu-dresden.de>
 * Example for scenery - swing bridge
 *
 * A TransferFunctionEditor example to add, manipulate and remove control points of a volume's transfer function.
 * Further more able to generate a histogram representation of the volume data distribution to help with the transfer function setup.
 *
 * Usage: To enable the UI on the plane click once (Key '1') while hovering over the plane. Key '1' used as normal Mouse-interactions (Clicking and dragging).
 * Control Points can be dragged, added and removed. A remove happens via Ctrl-Clicking (In this example managed by using Key '2'.
 */
class TransferFunctionEditorExample : DefaultScene() {
    var maxCacheSize = 512

    /**
     * Sets up the example, containing 2 light sources (PointLight), a perspective camera and a volume.
     * Also adds a SwingUINode containing a SwingBridgeFrame contained by a TransferFunctionUI to manipulate the Volume
     */
    override fun init() {
        super.init()

        val options = VolumeViewerOptions().maxCacheSizeInMB(maxCacheSize)
        //Currently only .xml volume formats are usable
        val v = Volume.fromXML("models/volumes/t1-head.xml", hub, options)
        v.name = "t1-head"
        v.colormap = Colormap.get("grays")
        v.spatial().position = Vector3f(0.0f, 0.0f, 0.0f)
        v.spatial().scale = Vector3f(0.1f)
        v.setTransferFunctionRange(0.0f, 1000.0f)
        scene.addChild(v)


        val bridge = SwingBridgeFrame("1DTransferFunctionEditor")
        val tfUI = TransferFunctionEditor(v, bridge)
        tfUI.name = v.name
        val swingUiNode = tfUI.mainFrame.uiNode
        swingUiNode.spatial() {
            position = Vector3f(2f,0f,0f)
        }

        scene.addChild(swingUiNode)
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

    /**
     * Static object for running as application
     */
    companion object {
        /**
         * Main method for the application, that instances and runs the example.
         */
        @JvmStatic
        fun main(args: Array<String>) {
            TransferFunctionEditorExample().main()
        }
    }
}


