package microscenery.VRUI.Gui3D

import microscenery.detach


class TabbedMenu(vararg tabs: Pair<String,Column>) : Column(invertedYOrder = false){
    val homeRow: Row
    val tabs: Array<Column>

    init {
        if(tabs.isEmpty()) throw IllegalArgumentException("Tabbed Menu needs at least one sub menu.")
        name = "TabbedMenu"


        this.tabs = tabs.map {it.second }.toTypedArray()
        this.tabs.forEach {
            it.middleAlign = false
        }

        homeRow = Row(*tabs.mapIndexed { index, pair -> toButton(index,pair.first) }.toTypedArray())
        this.addChild(homeRow)

        addChild(this.tabs[0])
        this.pack()
    }

    private fun toButton(index: Int, name:String): Button{
        return Button(name){
            tabs.forEach {
                it.detach()
            }
            addChild(tabs[index])
            this.pack()
        }
    }
}