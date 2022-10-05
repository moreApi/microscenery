package microscenery

import microscenery.network.HardwareDimensions
import microscenery.network.NumericType
import microscenery.network.ServerSignal
import org.joml.Vector2i
import org.joml.Vector3f
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import kotlin.math.sqrt

interface MicroscopeHardware{

    var stagePosition:Vector3f

    fun serverStatus(): ServerSignal.ServerStatus
    fun hardwareDimensions(): HardwareDimensions
    fun snapSlice()
    fun shutdown()


    val output: BlockingQueue<ServerSignal>
}

class DemoMicroscopyHardware(
    val slice: ByteBuffer,
    override var stagePosition: Vector3f = Vector3f(),
    val hardwareDimensions: HardwareDimensions = HardwareDimensions(
        Vector3f(-100f),
        Vector3f(100f),
        imageSize = Vector2i(sqrt(slice.capacity().toDouble()).toInt()),
        Vector3f(1f),
        NumericType.INT8
    )
): MicroscopeHardware{
    var idCounter = 0

    override val output: BlockingQueue<ServerSignal> = ArrayBlockingQueue(10)

    var status = ServerSignal.ServerStatus.EMPTY.copy(hwDimensions = hardwareDimensions)

    override fun serverStatus(): ServerSignal.ServerStatus {
        return status
    }

    override fun hardwareDimensions(): HardwareDimensions {
        return hardwareDimensions
    }

    override fun snapSlice() {

        val buffer = ByteBuffer.allocate(slice.capacity())
        buffer.put(slice)
        buffer.flip()

        val signal =ServerSignal.Slice(idCounter++,System.currentTimeMillis(),stagePosition,slice.capacity(),null,buffer)
        output.put(signal)
    }
    override fun shutdown() {    }

}