package microscenery.signals

import com.google.protobuf.util.Timestamps.fromMillis
import me.jancasus.microscenery.network.v2.EnumNumericType
import me.jancasus.microscenery.network.v2.EnumServerState
import microscenery.signals.HardwareDimensions.Companion.toPoko
import microscenery.signals.MicroscopeStatus.Companion.toPoko
import org.joml.Vector2i
import org.joml.Vector3f
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
                    TODO("stack")
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
                me.jancasus.microscenery.network.v2.MicroscopeSignal.SignalCase.HARDWAREDIMENSIONS ->{
                    this.hardwareDimensions.toPoko()
                }
            }
    }

}


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

data class Stack(val Id: Int) : MicroscopeSignal() {
    override fun toProto(): me.jancasus.microscenery.network.v2.MicroscopeSignal {
        TODO("Stack Not yet implemented")
    }
}

data class HardwareDimensions(
    val stageMin: Vector3f,
    val stageMax: Vector3f,
    val imageSize: Vector2i,
    val vertexSize: Vector3f,
    val numericType: NumericType
) : MicroscopeSignal() {
    override fun toProto(): me.jancasus.microscenery.network.v2.MicroscopeSignal {
        val microscopeSignal = me.jancasus.microscenery.network.v2.MicroscopeSignal.newBuilder()

        val hd = microscopeSignal.hardwareDimensionsBuilder
        hd.stageMin = this.stageMin.toProto()
        hd.stageMax = this.stageMax.toProto()
        hd.imageSize = this.imageSize.toProto()
        hd.vertexSize = this.vertexSize.toProto()
        hd.numericType = this.numericType.toProto()
        hd.build()

        return microscopeSignal.build()
    }

    val byteSize
        get() = imageSize.x * imageSize.y * numericType.bytes

    companion object {
        val EMPTY = HardwareDimensions(Vector3f(), Vector3f(), Vector2i(), Vector3f(), NumericType.INT16)

        fun me.jancasus.microscenery.network.v2.HardwareDimensions.toPoko(): HardwareDimensions {
            val hwd = this
            return HardwareDimensions(
                hwd.stageMin.toPoko(),
                hwd.stageMax.toPoko(),
                hwd.imageSize.toPoko(),
                hwd.vertexSize.toPoko(),
                hwd.numericType.toPoko()
            )
        }
    }
}


data class MicroscopeStatus(
    val state: ServerState,
    val stagePosition: Vector3f
) : MicroscopeSignal() {
    override fun toProto(): me.jancasus.microscenery.network.v2.MicroscopeSignal {
        val microscopeSignal = me.jancasus.microscenery.network.v2.MicroscopeSignal.newBuilder()

        val status = microscopeSignal.statusBuilder
        status.state = this.state.toProto()
        status.stagePosition = this.stagePosition.toProto()
        status.build()

        return microscopeSignal.build()
    }

    companion object {
        fun me.jancasus.microscenery.network.v2.MicroscopeStatus.toPoko(): MicroscopeStatus {
            val ss = this
            return MicroscopeStatus(
                ss.state.toPoko(),
                ss.stagePosition.toPoko()
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

