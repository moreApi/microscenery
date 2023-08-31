package microscenery.example

import graphics.scenery.RichNode
import microscenery.DefaultScene
import microscenery.UI.Row
import microscenery.UI.TextBox
import org.joml.Vector3f
import kotlin.concurrent.thread

class Playground() : DefaultScene() {
    override fun init() {
        super.init()


        val menu = RichNode()




        Row(
            TextBox("laser power")
        ).apply {
            spatial {
                position = Vector3f(0f, 0.6f, 0f)
            }

            menu.addChild(this)
        }


        Row(
            TextBox("laser power")
        ).apply {
            spatial {
                position = Vector3f(0f, 0.6f, 0f)
            }
            menu.addChild(this)
        }

        Row(
            TextBox("++"),
            TextBox("+"),
            TextBox("5"),
            TextBox("-"),
            TextBox("--")
        ).apply {
            spatial {
                position = Vector3f(0f, -0.6f, 0f)
            }
            menu.addChild(this)
        }

        scene.addChild(menu)


        thread {
            menu
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Playground().main()
        }
    }
}
