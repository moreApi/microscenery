package microscenery.VRUI.elements

import graphics.scenery.attribute.spatial.HasSpatial

interface Ui3DElement: HasSpatial {
    /**
     * Assuming no scaling
     */
    val width: Float

    /**
     * Assuming no scaling
     */
    val height: Float
}