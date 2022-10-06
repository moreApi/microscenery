package microscenery

import graphics.scenery.utils.LazyLogger
import graphics.scenery.volumes.Volume
import microscenery.network.HardwareDimensions
import microscenery.network.NumericType
import microscenery.network.ServerSignal
import microscenery.network.ServerState
import org.joml.Vector2i
import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

class DemoMicroscopyHardware(
    stagePosition: Vector3f = Vector3f(),
): MicroscopeHardware {
     protected val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

    override var stagePosition = stagePosition
        set(target) {
            val safeTarget = Vector3f()
            for (i in 0..2) safeTarget.setComponent(i, target[i].coerceIn(hardwareDimensions.stageMin[i], hardwareDimensions.stageMax[i]))
            if (safeTarget != target) {
                logger.warn("Had to coerce stage parameters! From $target to ${safeTarget}")
            }
            field = safeTarget
        }
    var idCounter = 0
    val side = 200
    val stageContent: ByteBuffer
    val hardwareDimensions: HardwareDimensions

    var status: ServerSignal.ServerStatus
    override val output: BlockingQueue<ServerSignal> = ArrayBlockingQueue(10)


    init {
        stageContent = Volume.generateProceduralVolume(size = side.toLong(), radius = 190f, use16bit = false)

        hardwareDimensions = HardwareDimensions(
            stageMin = Vector3f(0f),
            stageMax = Vector3f(side.toFloat()),
            imageSize = Vector2i(200),
            vertexSize = Vector3f(1f),
            numericType = NumericType.INT8
        )
        status = ServerSignal.ServerStatus(
            ServerState.MANUAL,
            emptyList(),
            0,
            hardwareDimensions
        )
        output.put(status)

    }

    override fun serverStatus(): ServerSignal.ServerStatus {
        return status
    }

    override fun hardwareDimensions(): HardwareDimensions {
        return hardwareDimensions
    }

    override fun snapSlice() {
        val imgX = hardwareDimensions.imageSize.x
        val imgY = hardwareDimensions.imageSize.y
        val sliceBuffer = MemoryUtil.memAlloc(imgX * imgY)

        val fullSliceSize = side * side
        val zOffset = fullSliceSize * stagePosition.z.toInt()
        stageContent.clear()
        stageContent.position(zOffset)
        stageContent.limit(zOffset+fullSliceSize)
        sliceBuffer.put(stageContent)

        /*

        for ( y in 0 until imgY){
            for (x in 0 until imgX){
                val yOffset = (stagePosition.y.toInt() + y)*side
                val xOffset = stagePosition.x.toInt() + x
                val pos = zOffset+yOffset+xOffset
                if (pos < 0 || stageContent.capacity() < pos){
                    sliceBuffer.put(Byte.MAX_VALUE)
                } else {
                    stageContent.position(pos)
                    sliceBuffer.put(stageContent.get())
                }
            }
        }
        */
        sliceBuffer.flip()

        val signal = ServerSignal.Slice(
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

}