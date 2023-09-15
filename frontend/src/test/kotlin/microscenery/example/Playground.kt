package microscenery.example

import microscenery.DefaultScene
import microscenery.Settings
import microscenery.VRUI.Gui3D.*
import kotlin.concurrent.thread

class Playground() : DefaultScene() {
    override fun init() {
        super.init()


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

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Playground().main()
        }
    }
}
