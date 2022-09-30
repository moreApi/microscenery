package microscenery

import graphics.scenery.RichNode
import graphics.scenery.Scene
import graphics.scenery.utils.extensions.times
import org.joml.Vector3f
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class StageSpaceManager(val hardware: MicroscopeHardware, val scene: Scene, val scaleDownFactor: Float = 200f) {

    val stageSpace = RichNode()

    val running = true

    init {
        thread {
            while (running && hardware.latestServerStatus==null){
                Thread.sleep(200)
            }
            hardware.latestServerStatus?.let {
                stageSpace.spatial().scale = it.hwDimensions.vertexSize.times(1/scaleDownFactor)
            }

            while (running){
                val (meta, data) = hardware.newSlice.poll(200,TimeUnit.MILLISECONDS) ?: continue
                val hwd = hardware.latestServerStatus?.hwDimensions ?: continue

                val node = SliceRenderNode(data,hwd.imageSize.x,hwd.imageSize.y,1f, hwd.numericType.bytes)
                node.spatial().position = meta.stagePos
                stageSpace.addChild(node)
            }
        }
    }


    fun snapSlice(target: Vector3f){
        hardware.moveStage(target)
        hardware.snapSlice()
    }
}

