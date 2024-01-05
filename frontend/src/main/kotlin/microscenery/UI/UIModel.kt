package microscenery.UI

import graphics.scenery.DefaultNode
import graphics.scenery.Node
import microscenery.PropertyChangeObservable

class UIModel : PropertyChangeObservable() {
    var selected: Node? by propertyObservable(null)
    fun updateSelected() {
        selected = selected
    }

    fun updateSelected(){selected = selected}

    companion object {
        // nullable generics are hard :(
        val NO_SELECTION = DefaultNode("No selection")
    }
}
