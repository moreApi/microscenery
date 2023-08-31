package microscenery.UI

import graphics.scenery.attribute.spatial.HasSpatial

interface Ui3DElement: HasSpatial {
    /**
     * Assuming no scaling
     */
    val width: Float
}