package microscenery

import graphics.scenery.*
import graphics.scenery.backends.Renderer
import graphics.scenery.utils.SceneryJPanel
import graphics.scenery.utils.extensions.times
import net.miginfocom.swing.MigLayout
import org.joml.Vector3f
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.JPanel

open class DefaultScene(
    val initHook: ((scene: Scene, hub: Hub) -> Unit)? = null, name: String = "Microscenery",
    width: Int = 600, height: Int = 600, val withSwingUI: Boolean = false
) : SceneryBase(name, wantREPL = false, windowWidth = width, windowHeight = height) {
    val cam: Camera = DetachedHeadCamera()
    var mainFrame: JFrame? = null
    var extraPanel: JPanel? = null

    override fun init() {
        if (withSwingUI) {
            mainFrame = JFrame(applicationName)
            mainFrame?.minimumSize = Dimension(windowWidth, windowHeight)
            mainFrame?.layout = BorderLayout()

            val sceneryPanel = SceneryJPanel()
            mainFrame?.add(sceneryPanel, BorderLayout.CENTER)
            mainFrame?.isVisible = true

            renderer = hub.add(
                SceneryElement.Renderer,
                Renderer.createRenderer(hub, applicationName, scene, windowWidth, windowHeight, embedIn = sceneryPanel)
            )
            renderer?.pushMode = true

            extraPanel = JPanel(MigLayout())
            mainFrame?.add(extraPanel!!, BorderLayout.EAST)
            mainFrame?.pack()
        } else {
            renderer = hub.add(
                SceneryElement.Renderer,
                Renderer.createRenderer(hub, applicationName, scene, windowWidth, windowHeight)
            )
        }

        val light = PointLight(radius = 15.0f)
        light.spatial().position = Vector3f(2.0f, 1.0f, 2.0f) * 2f
        light.intensity = 15.0f
        light.emissionColor = Vector3f(1.0f, 1.0f, 1.0f)
        scene.addChild(light)

        val light2 = PointLight(radius = 15.0f)
        light2.spatial().position = Vector3f(-2.0f, -1.0f, -2.0f) * 2f
        light2.intensity = 15.0f
        light2.emissionColor = Vector3f(1.0f, 1.0f, 1.0f)
        scene.addChild(light2)

        with(cam) {
            spatial {
                position = Vector3f(0.0f, 0.0f, 1.5f)
            }
            perspectiveCamera(50.0f, windowWidth, windowHeight)

            scene.addChild(this)
        }

        initHook?.invoke(scene, hub)
    }

    override fun close() {
        mainFrame?.dispose()
        super.close()
    }
}