package microscenery.UI

import graphics.scenery.DefaultNode
import graphics.scenery.Node

class UIModel: PropertyChangeObservable() {
    // nullable generics are hard :(
    val NO_SELECTION = DefaultNode("No selection")
    var selected: Node by propertyObservable<Node>(NO_SELECTION)
}
