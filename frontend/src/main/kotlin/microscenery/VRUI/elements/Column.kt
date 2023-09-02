package microscenery.VRUI.elements

import graphics.scenery.RichNode
import graphics.scenery.utils.lazyLogger

/**
 * Rows cousin. Anchor is bottom middle.
 */
class Column(vararg elements: Ui3DElement, val margin: Float = 0.2f, var middleAlign: Boolean = false)
: RichNode("UI Column"), Ui3DElement {
    override val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    override var width = 0f
        private set
    override var height = 0f
        private set

    init {
        elements.forEach { this.addChild(it) }
        update += {
            val uiChildren = children.filterIsInstance(Ui3DElement::class.java).reversed()
            val currentHeight = uiChildren.sumOf { 1.0 }.toFloat() + (uiChildren.size-1)*margin
            if (currentHeight != height){
                height = currentHeight
                var indexHeight = 0f
                uiChildren.forEach {
                    it.spatial(){
                        position.y = indexHeight
                        needsUpdate = true
                    }
                    indexHeight += 1f + margin
                }
                if (middleAlign){
                    indexHeight -= margin
                    spatial {
                        position.y = indexHeight * -0.5f
                        needsUpdate = true
                    }
                }
                width = uiChildren.maxOf { it.width }
            }
        }
    }
}