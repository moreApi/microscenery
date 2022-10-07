package microscenery.hardware

import graphics.scenery.utils.LazyLogger
import graphics.scenery.volumes.Volume
import microscenery.signals.*
import org.joml.Vector2i
import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

/**
 * Demo Hardware that works on generated by [Volume.generateProceduralVolume]
 */
class DemoMicroscopeHardware(
    stagePosition: Vector3f = Vector3f(),
): MicroscopeHardwareAgent() {
     protected val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

    var stagePosition = stagePosition
        set(target) {
            val safeTarget = Vector3f()
            for (i in 0..2) safeTarget.setComponent(i, target[i].coerceIn(hardwareDimensions.stageMin[i], hardwareDimensions.stageMax[i]))
            if (safeTarget != target) {
                logger.warn("Had to coerce stage parameters! From $target to ${safeTarget}")
            }
            field = safeTarget
            status = status.copy(stagePosition = safeTarget)
        }
    var idCounter = 0
    val side = 200
    val stageContent: ByteBuffer
    override val output: BlockingQueue<MicroscopeSignal> = ArrayBlockingQueue(10)
    init {
        stageContent = Volume.generateProceduralVolume(size = side.toLong(), radius = 190f, use16bit = false)

        hardwareDimensions = HardwareDimensions(
            stageMin = Vector3f(0f),
            stageMax = Vector3f(side.toFloat()),
            imageSize = Vector2i(50, 50),
            vertexSize = Vector3f(1f),
            numericType = NumericType.INT8
        )
        status = MicroscopeStatus(
            ServerState.MANUAL,
            stagePosition
        )

    }

    override fun snapSlice(target: Vector3f) {
        stagePosition = target
        val imgX = hardwareDimensions.imageSize.x
        val imgY = hardwareDimensions.imageSize.y
        val sliceBuffer = MemoryUtil.memAlloc(imgX * imgY)

        val fullSliceSize = side * side
        val zOffset = fullSliceSize * stagePosition.z.toInt()
        for (y in 0 until imgY){
            val yOffset = side * (stagePosition.y.toInt() + y)
            val xOffset = stagePosition.x.toInt()
            val offset = zOffset + yOffset + xOffset
            stageContent.limit((offset+imgX).coerceIn(0, stageContent.capacity()))
            stageContent.position(offset.coerceIn(0, stageContent.capacity()))
            sliceBuffer.put(stageContent)
        }

        sliceBuffer.clear()

        val signal = Slice(
            idCounter++,
            System.currentTimeMillis(),
            stagePosition,
            sliceBuffer.capacity(),
            null,
            sliceBuffer
        )
        output.put(signal)
    }

    override fun shutdown() {    }
    override fun onLoop() {
        throw NotImplementedError("demo hardware has no active agent")
    }
}
