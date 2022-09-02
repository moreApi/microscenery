package microscenery.example

import graphics.scenery.BoundingGrid
import graphics.scenery.primitives.Line
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import microscenery.DefaultScene
import microscenery.DefaultVRScene
import microscenery.VRUI.swingBridge.SwingBridgeFrame
import microscenery.VRUI.swingBridge.SwingUiNode
import microscenery.VRUI.swingBridge.VRUICursor
import org.apache.commons.io.filefilter.FalseFileFilter
import org.apache.commons.lang.ObjectUtils
import org.joml.Quaternionf
import org.joml.Vector3f
import org.joml.Vector4f
import org.scijava.ui.behaviour.ClickBehaviour
import java.awt.Color
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.JButton


/**
 * Example for scenery - swing bridge
 *
 * Aim with mouse and press 1 to "click"
 *
 * For Vr version uncomment next line and VRCursor setup in inputSetup method
 */
//class SwingBridge : DefaultVRScene("Main") {
class SwingBridge : DefaultScene() {

    override fun init() {
        super.init()

        cam.spatial().position = Vector3f(0f,0f,2f)
        cam.nearPlaneDistance = 0.01f

        val swingUiNode = SwingUiNode(DemoSwingUI())
        swingUiNode.spatial(){
//            position = Vector3f(1f,1f,-1f)
//            rotation = Quaternionf().lookAlong(Vector3f(-1f,-1f,1f), UP)
//            scale = Vector3f(2f)
        }
        scene.addChild(swingUiNode)

        val line = Line(50, false, true)
        line.name = "Line"
        line.edgeWidth = 3.0f
        line.lineColor = Vector4f(1.0f, 1.0f, 0.0f, 1.0f)
        scene.addChild(line)
    }

    override fun inputSetup() {
        super.inputSetup()

        inputHandler?.addBehaviour(
            "sphereDragObject", object : ClickBehaviour {
                override fun click(x: Int, y: Int) {
                    val ray = cam.getNodesForScreenSpacePosition(x,y, listOf<Class<*>>(BoundingGrid::class.java), true)
                    val line = scene.find("Line") as? Line
                    if(line is Line)
                    {
                        line.addPoint(cam.spatial().position)
                        line.addPoint(ray.initialPosition + ray.initialDirection.times(5.0f))
                    }

                    ray.matches.firstOrNull()?.let { hit ->
                        val node = hit.node as? SwingUiNode ?: hit.node.parent as? SwingUiNode ?: return //backside might get hit first
                        val hitPos = ray.initialPosition + ray.initialDirection.normalize(hit.distance)
                        logger.info("Distance: ${hit.distance}")
                        node.click(hitPos)
                    }
                }
            }
        )
        inputHandler?.addKeyBinding("sphereDragObject", "1")

//        VRUICursor.createAndSet(scene,hmd,
//            listOf( OpenVRHMD.OpenVRButton.Trigger),
//            listOf(TrackerRole.LeftHand,TrackerRole.RightHand))
    }


    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SwingBridge().main()
        }
    }
}

class DemoSwingUI(): SwingBridgeFrame("menu"){
    init {
        val mainFrame = this
        mainFrame.layout = GridLayout(2,2)
        mainFrame.background = Color(0,100,0)
        mainFrame.getContentPane().setPreferredSize(Dimension(200, 200))
        mainFrame.pack()


        val but = JButton("klick")
        var co = 0
        but.addActionListener {
            but.text = "klick" + co++
            println("button 0 clicked")
        }

        mainFrame.add(but)

        (1..3).forEach {
            val but2 = JButton( "but"+it)
            but2.addActionListener { _ ->
                println("button $it clicked")
            }

            mainFrame.add(but2)
        }
        mainFrame.isVisible = true
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            DemoSwingUI()
        }
    }
}


