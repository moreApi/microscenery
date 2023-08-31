package microscenery.VRUI.elements

import graphics.scenery.RichNode
import graphics.scenery.utils.lazyLogger

/**
 * Currently assumes elements are mirrord in size
 */
class Row(vararg elements: Ui3DElement, val margin: Float = 0.5f, var middleAlign: Boolean = true)
    : RichNode("UI Row"), Ui3DElement {
    override val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    override var width = 0f
        private set

    init {
        elements.forEach { this.addChild(it) }
        update += {
            val uiChildren = children.filterIsInstance(Ui3DElement::class.java)
            if (uiChildren.size % 2 == 0)
                logger.warn("Currenty not implement for an even ammount of children. Results may vary")
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
            }
        }
    }

    fun forceUpdate() {
        width = -1f
    }
}