package microscenery.signals

import com.google.protobuf.util.Timestamps.fromMillis
import me.jancasus.microscenery.network.v2.EnumNumericType
import me.jancasus.microscenery.network.v2.EnumServerState
import microscenery.signals.HardwareDimensions.Companion.toPoko
import microscenery.signals.MicroscopeStatus.Companion.toPoko
import microscenery.toReadableString
import org.joml.Vector2i
import org.joml.Vector3f
import org.joml.Vector3i
import java.nio.ByteBuffer


sealed class MicroscopeSignal {

    abstract fun toProto(): me.jancasus.microscenery.network.v2.MicroscopeSignal

    companion object {
        fun me.jancasus.microscenery.network.v2.MicroscopeSignal.toPoko() =
            when (this.signalCase ?: throw IllegalArgumentException("Illegal payload")) {
                me.jancasus.microscenery.network.v2.MicroscopeSignal.SignalCase.STATUS -> {
                    this.status.toPoko()
                }
                me.jancasus.microscenery.network.v2.MicroscopeSignal.SignalCase.STACK -> {
                    val s = this.stack
                    Stack(
                        s.id,
                        s.live,
                        s.stageMin.toPoko(),
                        s.size.toPoko(),
                        s.created.seconds * 1000 + s.created.nanos.div(1000),
                        s.voxelSize.toPoko()
                    )
                }
                me.jancasus.microscenery.network.v2.MicroscopeSignal.SignalCase.SLICE -> {
                    val s = this.slice
                    Slice(
                        s.id,
                        s.created.seconds * 1000 + s.created.nanos.div(1000),
                        s.stagePos.toPoko(),
                        s.size,
                        if (s.stackId == -1) null else s.stackId,
                        null
                    )
                }
                me.jancasus.microscenery.network.v2.MicroscopeSignal.SignalCase.SIGNAL_NOT_SET ->
                    throw IllegalArgumentException("Signal is not set in Server signal message")
                me.jancasus.microscenery.network.v2.MicroscopeSignal.SignalCase.HARDWAREDIMENSIONS -> {
                    this.hardwareDimensions.toPoko()
                }
            }
    }
}

/**
 * @param size size of the slice in bytes
 */
data class Slice(
    val Id: Int,
    val created: Long,
    val stagePos: Vector3f,
    val size: Int,
    val stackId: Int?,
    val data: ByteBuffer?

) : MicroscopeSignal() {
    override fun toProto(): me.jancasus.microscenery.network.v2.MicroscopeSignal {
        val microscopeSignal = me.jancasus.microscenery.network.v2.MicroscopeSignal.newBuilder()

        val s = microscopeSignal.sliceBuilder
        s.id = this.Id
        s.created = fromMillis(this.created)
        s.stagePos = this.stagePos.toProto()
        s.size = this.size
        s.stackId = this.stackId ?: -1
        s.build()

        return microscopeSignal.build()
    }
}

data class Stack(
    val Id: Int,
    val live: Boolean,
    val stageMin: Vector3f,
    val size: Vector3i,
    val created: Long,
    val voxelSize: Vector3f
) : MicroscopeSignal() {
    override fun toProto(): me.jancasus.microscenery.network.v2.MicroscopeSignal {
        val microscopeSignal = me.jancasus.microscenery.network.v2.MicroscopeSignal.newBuilder()

        val s = microscopeSignal.stackBuilder
        s.id = this.Id
        s.live = this.live
        s.created = fromMillis(this.created)
        s.stageMin = this.stageMin.toProto()
        s.size = this.size.toProto()
        s.voxelSize = this.voxelSize.toProto()
        s.build()

        return microscopeSignal.build()
    }
}

data class HardwareDimensions(
    val stageMin: Vector3f,
    val stageMax: Vector3f,
    val imageSize: Vector2i,
    val vertexDiameter: Float,
    val numericType: NumericType
) : MicroscopeSignal() {
    override fun toProto(): me.jancasus.microscenery.network.v2.MicroscopeSignal {
        val microscopeSignal = me.jancasus.microscenery.network.v2.MicroscopeSignal.newBuilder()

        val hd = microscopeSignal.hardwareDimensionsBuilder
        hd.stageMin = this.stageMin.toProto()
        hd.stageMax = this.stageMax.toProto()
        hd.imageSize = this.imageSize.toProto()
        hd.vertexDiameter = this.vertexDiameter
        hd.numericType = this.numericType.toProto()
        hd.build()

        return microscopeSignal.build()
    }

    val byteSize
        get() = imageSize.x * imageSize.y * numericType.bytes

    companion object {
        val EMPTY = HardwareDimensions(Vector3f(), Vector3f(), Vector2i(), 1f, NumericType.INT16)

        fun me.jancasus.microscenery.network.v2.HardwareDimensions.toPoko(): HardwareDimensions {
            val hwd = this
            return HardwareDimensions(
                hwd.stageMin.toPoko(),
                hwd.stageMax.toPoko(),
                hwd.imageSize.toPoko(),
                hwd.vertexDiameter,
                hwd.numericType.toPoko()
            )
        }
    }

    /**
     * Limits a position within the stage limits.
     *
     * @param safetyCutoff If the distance in micrometer to the next legal position is larger than this value, a error is thrown to abort the operation as a safety measure.
     */
    fun coercePosition(
        target: Vector3f,
        logger: org.slf4j.Logger?,
        safetyCutoff: Vector3f = Vector3f(1000f)
    ): Vector3f {
        val safeTarget = Vector3f()
        for (i in 0..2) {
            safeTarget.setComponent(i, target[i].coerceIn(stageMin[i], stageMax[i]))
            if (safeTarget[i] - target[i] > safetyCutoff[i]) {
                val message = "stage position fail safe triggered. Target  ${target.toReadableString()} is " +
                        "more than the safety distance of ${safetyCutoff.toReadableString()} away from the allowed area " +
                        "between ${stageMin.toReadableString()} and ${stageMax.toReadableString()}."
                logger?.error(message)
                throw IllegalStateException(message)
            }
        }
        if (safeTarget != target) {


            logger?.warn("Had to coerce stage parameters! From ${target.toReadableString()} to ${safeTarget.toReadableString()}")
        }
        return safeTarget
    }
}


data class MicroscopeStatus(
    val state: ServerState,
    val stagePosition: Vector3f,
    val live: Boolean
) : MicroscopeSignal() {
    override fun toProto(): me.jancasus.microscenery.network.v2.MicroscopeSignal {
        val microscopeSignal = me.jancasus.microscenery.network.v2.MicroscopeSignal.newBuilder()

        val status = microscopeSignal.statusBuilder
        status.state = this.state.toProto()
        status.stagePosition = this.stagePosition.toProto()
        status.live = this.live
        status.build()

        return microscopeSignal.build()
    }

    companion object {
        fun me.jancasus.microscenery.network.v2.MicroscopeStatus.toPoko(): MicroscopeStatus {
            val ss = this
            return MicroscopeStatus(
                ss.state.toPoko(),
                ss.stagePosition.toPoko(),
                ss.live
            )
        }
    }
}

enum class ServerState {
    LIVE, MANUAL, SHUTTING_DOWN, STACK, STARTUP,
}

fun ServerState.toProto() = when (this) {
    ServerState.LIVE -> EnumServerState.SERVER_STATE_LIVE
    ServerState.MANUAL -> EnumServerState.SERVER_STATE_MANUAL
    ServerState.SHUTTING_DOWN -> EnumServerState.SERVER_STATE_SHUTTING_DOWN
    ServerState.STACK -> EnumServerState.SERVER_STATE_STACK
    ServerState.STARTUP -> EnumServerState.SERVER_STATE_STARTUP
}

fun EnumServerState.toPoko() = when (this) {
    EnumServerState.SERVER_STATE_UNKNOWN -> throw IllegalArgumentException("Cant convert to ServerState")
    EnumServerState.SERVER_STATE_LIVE -> ServerState.LIVE
    EnumServerState.SERVER_STATE_MANUAL -> ServerState.MANUAL
    EnumServerState.SERVER_STATE_SHUTTING_DOWN -> ServerState.SHUTTING_DOWN
    EnumServerState.SERVER_STATE_STACK -> ServerState.STACK
    EnumServerState.SERVER_STATE_STARTUP -> ServerState.STARTUP
    EnumServerState.UNRECOGNIZED -> throw IllegalArgumentException("Cant convert to ServerState")
}

enum class NumericType(val bytes: Int) {
    INT8(1),
    INT16(2)
}

fun NumericType.toProto() = when (this) {
    NumericType.INT8 -> EnumNumericType.VALUE_NUMERIC_INT8
    NumericType.INT16 -> EnumNumericType.VALUE_NUMERIC_INT16
}

fun EnumNumericType.toPoko() = when (this) {
    EnumNumericType.VALUE_NUMERIC_UNKNOWN -> throw IllegalArgumentException("Cant convert to NumericType")
    EnumNumericType.VALUE_NUMERIC_INT8 -> NumericType.INT8
    EnumNumericType.VALUE_NUMERIC_INT16 -> NumericType.INT16
    EnumNumericType.UNRECOGNIZED -> throw IllegalArgumentException("Cant convert to NumericType")
}

