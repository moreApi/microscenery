package microscenery.example

import microscenery.DefaultScene
import microscenery.VRUI.elements.ValueEdit
import kotlin.concurrent.thread

class Playground() : DefaultScene() {
    override fun init() {
        super.init()


        val menu = ValueEdit(0,{it+1},{it-1},{it+10},{it-10})
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
