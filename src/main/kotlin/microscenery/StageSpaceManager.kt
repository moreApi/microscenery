package microscenery

import graphics.scenery.RichNode
import graphics.scenery.Scene
import graphics.scenery.utils.extensions.times
import microscenery.hardware.MicroscopeHardware
import microscenery.signals.HardwareDimensions
import microscenery.signals.Slice
import org.joml.Vector3f
import java.util.concurrent.TimeUnit

/**
 * Handles in and output events concerning the microscope.
 *
 * Constructor waits until microscope is ready.
 */
class StageSpaceManager(val hardware: MicroscopeHardware, val scene: Scene, val scaleDownFactor: Float = 200f) :
    Agent() {

    val stageRoot = RichNode("Stage root")

    init {
        scene.addChild(stageRoot)

        var signal = hardware.output.poll() as? HardwareDimensions
        while (running && signal == null) {
            signal = hardware.output.poll() as? HardwareDimensions
        }
        signal?.let {
            stageRoot.spatial().scale = signal.vertexSize.times(1 / scaleDownFactor)
            startAgent()
        }
    }

    override fun onLoop() {
        val signal = hardware.output.poll(200, TimeUnit.MILLISECONDS)
        when (signal) {
            is Slice -> {
                if (signal.data == null) return
                val hwd = hardware.hardwareDimensions()

                val node = SliceRenderNode(signal.data, hwd.imageSize.x, hwd.imageSize.y, 1f, hwd.numericType.bytes)
                node.spatial().position = signal.stagePos
                stageRoot.addChild(node)
            }
            else -> {}
        }

    }


    fun snapSlice(target: Vector3f) {
        hardware.snapSlice(target)
    }
}

