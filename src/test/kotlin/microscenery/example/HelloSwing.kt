package microscenery.example

import copy
import graphics.scenery.*
import graphics.scenery.attribute.material.Material
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackedDeviceType
import graphics.scenery.controls.behaviours.Selectable
import graphics.scenery.controls.behaviours.VRSelect
import graphics.scenery.primitives.Cylinder
import graphics.scenery.primitives.LineBetweenNodes
import graphics.scenery.primitives.Plane
import graphics.scenery.textures.Texture
import graphics.scenery.utils.Image
import graphics.scenery.utils.Wiggler
import graphics.scenery.utils.extensions.plus
import microscenery.DefaultScene
import microscenery.DefaultVRScene
import org.joml.Vector3f
import org.joml.Vector4f
import org.scijava.ui.behaviour.ClickBehaviour
import org.scijava.ui.behaviour.DragBehaviour
import java.awt.*
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.image.BufferedImage
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.concurrent.thread
import kotlin.math.PI


class HelloSwing {

    val mainFrame = JFrame("lol")

    init {
        mainFrame.layout = GridLayout(2,2)
        mainFrame.background = Color(0,100,0)
        mainFrame.getContentPane().setPreferredSize(Dimension(200, 200))
        mainFrame.pack()

        val mlistener= object: MouseListener {
            override fun mouseClicked(e: MouseEvent?) {
                println(e)
            }

            override fun mousePressed(e: MouseEvent?) {
                println(e)
            }

            override fun mouseReleased(e: MouseEvent?) {
                println(e)
            }

            override fun mouseEntered(e: MouseEvent?) {
//                println(e)
            }

            override fun mouseExited(e: MouseEvent?) {
//                println(e)
            }

        }

        val but = JButton("klick")
        var co = 0
        but.addActionListener(ActionListener {
            but.text = "klick"+co++
            println("button 0 clicked")
        })
//        but.addMouseListener(mlistener)
        mainFrame.add(but)

        (1..3).forEach {
            val but2 = JButton( "but"+it)
            but2.addActionListener(ActionListener { _ ->
                println("button $it clicked")
            })
//            but2.addMouseListener(mlistener)
            mainFrame.add(but2)

        }


//        mainFrame.addMouseMotionListener(object : MouseMotionListener{
//            override fun mouseDragged(e: MouseEvent?) {
//                println(e)
//            }
//
//            override fun mouseMoved(e: MouseEvent?) {
//                println(e)
//            }
//
//        })
//




        //mainFrame.dispatchEvent(MouseEvent())
        mainFrame.isVisible = true
    }

    fun click(x: Int, y: Int){
        SwingUtilities.invokeLater {
            val target = SwingUtilities.getDeepestComponentAt(mainFrame.contentPane, x, y)
            println("SwingUI: simulating Click at $x,$y on ${(target as? JButton)?.text}")
            val compPoint = SwingUtilities.convertPoint(
                mainFrame.contentPane,
                x,y,target)

            // entered
            target.dispatchEvent(
                MouseEvent(
                    target, 504, System.currentTimeMillis() - 100,
                    0, compPoint.x,compPoint.y, 0, false, 0
                )
            )
            // pressed
            target.dispatchEvent(
                MouseEvent(
                    target, 501, System.currentTimeMillis() - 75,
                    1040, compPoint.x,compPoint.y, 1, false, 1
                )
            )
            // released
            target.dispatchEvent(
                MouseEvent(
                    target, 502, System.currentTimeMillis() - 50,
                    16, compPoint.x,compPoint.y, 1, false, 1
                )
            )
            // clicked
            target.dispatchEvent(
                MouseEvent(
                    target, 500, System.currentTimeMillis() - 25,
                    16, compPoint.x,compPoint.y, 1, false, 1
                )
            )
            // exited
            target.dispatchEvent(
                MouseEvent(
                    target, 505, System.currentTimeMillis(),
                    0, compPoint.x,compPoint.y, 0, false, 0
                )
            )
        }
    }

    fun getScreen(): BufferedImage {
        return getScreenShot(mainFrame.contentPane)
    }
    private fun getScreenShot(
        component: Component
    ): BufferedImage {
        val image = BufferedImage(
            component.getWidth(),
            component.getHeight(),
            BufferedImage.TYPE_INT_RGB
        )
        // call the Component's paint method, using
        // the Graphics object of the image.
        component.paint(image.graphics) // alternately use .printAll(..)
        return image
    }


    companion object {

        object GUIStarter {
            @JvmStatic
            fun main(args: Array<String>) {
                val hs = HelloSwing()
                thread {
                    Thread.sleep(500)
                    //hs.click(103,44)
                    //hs.getScreen()
                }
            }
        }
    }
}

class TexturedCubeExample2 : DefaultVRScene("Main") {
//class TexturedCubeExample2 : DefaultScene() {
    val hs = HelloSwing()
    val swingUiNode = Box(Vector3f(1.0f, 1.0f, 0.1f))

    var swingUiDimension = 0 to 0

    fun updateUITexture(){
        val bimage = hs.getScreen()
        val flipped = Image.createFlipped(bimage)
        val buffer = Image.bufferedImageToRGBABuffer(flipped)
        val final = Image(buffer,bimage.width,bimage.height)
        swingUiNode.material {
            textures["diffuse"] = Texture.fromImage(final)
        }
        swingUiDimension = bimage.width to bimage.height
    }

    override fun init() {
        super.init()

        cam.spatial().position = Vector3f(0f,0f,2f)

        swingUiNode.name = "le box du win"
        swingUiNode.material {
            metallic = 0f
            roughness = 0f
        }
        thread {
            while (true) {
                updateUITexture()
                Thread.sleep(200)
            }
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
    }

    override fun inputSetup() {
        super.inputSetup()

        inputHandler?.addBehaviour(
            "sphereDragObject", object : ClickBehaviour {
                override fun click(x: Int, y: Int) {
//                  hs.click(50, 50)
                    val ray = cam.getNodesForScreenSpacePosition(x,y)
                    ray.matches.firstOrNull { it.node == swingUiNode }?.let { hit ->
                        val hitPos = ray.initialPosition + ray.initialDirection.normalize(hit.distance)
                        val hitPosModel = Vector4f(hitPos,1f).mul(swingUiNode.spatial().model.copy().invert())
                        println("${hitPosModel.x},${hitPosModel.y},${hitPosModel.z},${hitPosModel.w},")

                        val swingX = (hitPosModel.x +0.5f) * swingUiDimension.first
                        val swingY = swingUiDimension.second - (hitPosModel.y +0.5f) * swingUiDimension.second
                        println("Cliky at $swingX : $swingY")
                        hs.click(swingX.toInt(),swingY.toInt())
                    }
                }
            }
        )
        inputHandler?.addKeyBinding("sphereDragObject", "1")

        val button = OpenVRHMD.OpenVRButton.Trigger
        hmd.events.onDeviceConnect.add { _, device, _ ->
            if (device.type == TrackedDeviceType.Controller) {
                device.model?.let { controller ->
//                    if (controllerSide.contains(device.role)) {
                        val name = "VRDrag:${hmd.trackingSystemName}:${device.role}:$button"
                        val select = VRUICursor(
                            name,
                            controller.children.first(),
                            scene,
                            this
                        )
                        hmd.addBehaviour(name, select)
//                        button.forEach {
                            hmd.addKeyBinding(name, device.role, button)
//                        }
//                    }
                }
            }
        }
    }


    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            TexturedCubeExample2().main()
        }
    }
}


open class VRUICursor(
    protected val name: String,
    protected val controller: Node,
    protected val scene: Scene,
    val ex: TexturedCubeExample2
) : DragBehaviour {


    private val laser = Cylinder(0.0025f, 1f, 20)
    private val selectionIndicator: LineBetweenNodes

    init {
        laser.material().diffuse = Vector3f(5.0f, 0.0f, 0.02f)
        laser.material().metallic = 0.0f
        laser.material().roughness = 1.0f
        laser.spatial().rotation.rotateX(-PI.toFloat() * 1.25f / 2.0f)
        laser.visible = false

        if (controller.spatialOrNull() == null) {
            throw IllegalArgumentException("The controller needs to have a spatial property!")
        }

        controller.addChild(laser)
        selectionIndicator = LineBetweenNodes(
            laser.spatial(), laser.spatial(),
            transparent = false,
            simple = true
        )
        selectionIndicator.visible = false
        scene.addChild(selectionIndicator)
    }

    /**
     * Activates the target las0r.
     */
    override fun init(x: Int, y: Int) {
        laser.visible = true
    }

    /**
     * Wiggles potential targets and adjust the length of the target laser visualisation.
     */
    override fun drag(x: Int, y: Int) {
        val hit = scene.raycast(
            controller.spatialOrNull()!!.worldPosition(),
            laser.spatial().worldRotation().transform(Vector3f(0f, 1f, 0f))
        )
            .matches.firstOrNull { it.node.getAttributeOrNull(Selectable::class.java) != null }


        laser.spatial().scale.y = hit?.distance ?: 1000f

        val hitSpatial = hit?.node?.spatialOrNull()
    }

    /**
     * Performs the selection
     */
    override fun end(x: Int, y: Int) {

        laser.visible = false

        val ray = scene.raycast(
            controller.spatialOrNull()!!.worldPosition(),
            laser.spatial().worldRotation().transform(Vector3f(0f, 1f, 0f))
        )
            ray.matches.firstOrNull { it.node == ex.swingUiNode }?.let { hit ->
            val hitPos = ray.initialPosition + ray.initialDirection.normalize(hit.distance)
            val hitPosModel = Vector4f(hitPos,1f).mul(ex.swingUiNode.spatial().model.copy().invert())
            println("${hitPosModel.x},${hitPosModel.y},${hitPosModel.z},${hitPosModel.w},")

            val swingX = (hitPosModel.x +0.5f) * ex.swingUiDimension.first
            val swingY = ex.swingUiDimension.second - (hitPosModel.y +0.5f) * ex.swingUiDimension.second
            println("Cliky at $swingX : $swingY")
            ex.hs.click(swingX.toInt(),swingY.toInt())
        }
    }
}