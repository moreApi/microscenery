package anon.stageSpace

import graphics.scenery.attribute.spatial.HasSpatial

/**
 * Syncs the world position of [stageNode] with the position of target.
 */
class WorldToLocalPosSync(val stageNode: HasSpatial, val target: HasSpatial) {
    init {
        activate()
        stageNode.addAttribute(WorldToLocalPosSync::class.java, this)
    }

    private fun setPos(){
        target.spatial().position = stageNode.spatial().worldPosition()
    }

    fun activate() {
        stageNode.update += this::setPos
    }

    fun deactivate(){
        stageNode.update -= this::setPos
    }
}