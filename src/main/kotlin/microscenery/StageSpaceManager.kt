package microscenery

import graphics.scenery.RichNode
import graphics.scenery.Scene
import graphics.scenery.utils.extensions.times
import microscenery.network.ServerSignal
import microscenery.network.ServerState
import org.joml.Vector3f
import java.util.concurrent.TimeUnit

/**
 * Handles in and output events concerning the microscope.
 *
 * Constructor waits until microscope is ready.
 */
class StageSpaceManager(val hardware: MicroscopeHardware, val scene: Scene, val scaleDownFactor: Float = 200f): Agent() {

    val stageSpace = RichNode("Stage root")

    init {
        scene.addChild(stageSpace)

        while (running && hardware.serverStatus().state != ServerState.STARTUP){
            Thread.sleep(200)
        }
        hardware.serverStatus().let {
            stageSpace.spatial().scale = it.hwDimensions.vertexSize.times(1/scaleDownFactor)
        }
        startAgent()
    }

    override fun onLoop() {
        val signal = hardware.output.poll(200,TimeUnit.MILLISECONDS)
                as? ServerSignal.Slice ?: return
        if (signal.data == null ) return
        val hwd = hardware.hardwareDimensions()

        val node = SliceRenderNode(signal.data,hwd.imageSize.x,hwd.imageSize.y,1f, hwd.numericType.bytes)
        node.spatial().position = signal.stagePos
        stageSpace.addChild(node)

    }


    fun snapSlice(target: Vector3f){
        hardware.stagePosition = target
        hardware.snapSlice()
    }
}

