package microscenery.VRUI.elements

import graphics.scenery.RichNode
import graphics.scenery.utils.lazyLogger


open class Row(vararg elements: Ui3DElement, val margin: Float = 0.5f, var middleAlign: Boolean = true)
    : RichNode("UI Row"), Ui3DElement {
    override val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    final override var width = 0f
        private set
    final override var height = 0f
        private set

    init {
        elements.forEach { this.addChild(it) }
        update += {
            val uiChildren = children.filterIsInstance(Ui3DElement::class.java)
            val currentWidth = uiChildren.sumOf { it.width.toDouble() }.toFloat() + (uiChildren.size-1)*margin
            if (currentWidth != width){
                width = currentWidth
                var indexWidth = 0f
                uiChildren.forEach {
                    it.spatial(){
                        position.x = indexWidth
                        needsUpdate = true
                    }
                    indexWidth += it.width + margin
                }
                if (middleAlign){
                    indexWidth -= margin
                    spatial {
                        position.x = indexWidth * -0.5f
                        needsUpdate = true
                    }
                }
                height = uiChildren.maxOf { it.height }
            }
        }
    }

    fun forceUpdate() {
        width = -1f
    }
}