package microscenery.VRUI.Gui3D

import microscenery.detach


class TabbedMenu(vararg tabs: Pair<String,Column>) : Column(invertedYOrder = false){
    val buttons: Array<Button>
    val subMenus: Array<Column>

    init {
        if(tabs.isEmpty()) throw IllegalArgumentException("Tabbed Menu needs at least one sub menu.")
        name = "TabbedMenu"


        this.subMenus = tabs.map {it.second }.toTypedArray()
        this.subMenus.forEach {
            it.middleAlign = false
        }

        buttons = tabs.mapIndexed { index, pair -> toButton(index,pair.first) }.toTypedArray()

        this.addChild(Row(*buttons))

        addChild(this.subMenus[0])
        buttons[0].pressed = true
        this.pack()
    }

    private fun toButton(index: Int, name:String): Button{
        return Button(name){
            subMenus.forEach {
                it.detach()
            }
            addChild(subMenus[index])

            buttons.forEach { it.pressed = false }
            buttons[index].pressed = true

            this.pack()
        }.apply {
            this.stayPressed = true
        }
    }
}