package microscenery.signals

import com.google.protobuf.util.Timestamps.fromMillis
import fromScenery.utils.extensions.minus
import fromScenery.utils.extensions.plus
import fromScenery.utils.extensions.times
import me.jancasus.microscenery.network.v2.EnumNumericType
import me.jancasus.microscenery.network.v2.EnumServerState
import microscenery.signals.HardwareDimensions.Companion.toPoko
import microscenery.signals.MicroscopeStatus.Companion.toPoko
import microscenery.toReadableString
import org.joml.Vector2f
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
                    val s = this.stack
                    Stack(
                        s.id,
                        s.live,
                        s.from.toPoko(),
                        s.to.toPoko(),
                        s.slicesCount,
                        s.created.seconds * 1000 + s.created.nanos.div(1000)
                    )
                }
                me.jancasus.microscenery.network.v2.MicroscopeSignal.SignalCase.SLICE -> {
                    val s = this.slice
                    Slice(
                        s.id,
                        s.created.seconds * 1000 + s.created.nanos.div(1000),
                        s.stagePos.toPoko(),
                        s.size,
                        if (s.stackId == -1) null else s.stackId to s.stackSliceIndex,
                        null
                    )
                }
                me.jancasus.microscenery.network.v2.MicroscopeSignal.SignalCase.SIGNAL_NOT_SET ->
                    throw IllegalArgumentException("Signal is not set in Server signal message")
                me.jancasus.microscenery.network.v2.MicroscopeSignal.SignalCase.HARDWAREDIMENSIONS -> {
                    this.hardwareDimensions.toPoko()
                }
                me.jancasus.microscenery.network.v2.MicroscopeSignal.SignalCase.ABLATIONRESULTS -> {
                    val ar = this.ablationResults
                    AblationResults(ar.totalTimeMillis,ar.perPointTimeList)
                }
            }
    }
}

/**
 * @param size size of the slice in bytes
 * @param stackIdAndSliceIndex is null if not associated with a stack
 */
data class Slice(
    val Id: Int,
    val created: Long,
    val stagePos: Vector3f,
    val size: Int,
    val stackIdAndSliceIndex: Pair<Int, Int>?,
    val data: ByteBuffer?
) : MicroscopeSignal() {
    override fun toProto(): me.jancasus.microscenery.network.v2.MicroscopeSignal {
        val microscopeSignal = me.jancasus.microscenery.network.v2.MicroscopeSignal.newBuilder()

        val s = microscopeSignal.sliceBuilder
        s.id = this.Id
        s.created = fromMillis(this.created)
        s.stagePos = this.stagePos.toProto()
        s.size = this.size
        s.stackId = this.stackIdAndSliceIndex?.first ?: -1
        s.stackSliceIndex = this.stackIdAndSliceIndex?.second ?: -1
        s.build()

        return microscopeSignal.build()
    }
}

data class Stack(
    val Id: Int,
    val live: Boolean,
    val from: Vector3f,
    val to: Vector3f,
    val slicesCount: Int,
    val created: Long
) : MicroscopeSignal() {
    override fun toProto(): me.jancasus.microscenery.network.v2.MicroscopeSignal {
        val microscopeSignal = me.jancasus.microscenery.network.v2.MicroscopeSignal.newBuilder()

        val s = microscopeSignal.stackBuilder
        s.id = this.Id
        s.live = this.live
        s.created = fromMillis(this.created)
        s.from = this.from.toProto()
        s.to = this.to.toProto()
        s.slicesCount = this.slicesCount
        s.build()

        return microscopeSignal.build()
    }
}

data class HardwareDimensions(
    val stageMin: Vector3f,
    val stageMax: Vector3f,
    val imageSize: Vector2i,
    val vertexDiameter: Float, // pixel to stage coord ratio
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
     */
    fun coercePosition(
        target: Vector3f,
        logger: org.slf4j.Logger?,
        inImageSpace: Boolean = false
    ): Vector3f {
        val safeTarget = Vector3f()

        val min = if (!inImageSpace)
            stageMin
        else
            stageMin - Vector3f(Vector2f(imageSize) * vertexDiameter * 0.5f, 0f)

        val max = if (!inImageSpace)
            stageMax
        else
            stageMax + Vector3f(Vector2f(imageSize) * vertexDiameter * 0.5f, 0f)

        for (i in 0..2) {
            safeTarget.setComponent(i, target[i].coerceIn(min[i], max[i]))
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

data class AblationResults(val totalTimeMillis: Int, val perPointTime: List<Int>) : MicroscopeSignal() {
    override fun toProto(): me.jancasus.microscenery.network.v2.MicroscopeSignal {
        val microscopeSignal = me.jancasus.microscenery.network.v2.MicroscopeSignal.newBuilder()
        val ar = microscopeSignal.ablationResultsBuilder
        ar.totalTimeMillis = totalTimeMillis
        ar.addAllPerPointTime(perPointTime)
        return microscopeSignal.build()
    }

    fun mean():Int = perPointTime.reduce{ acc, i -> acc + i }.div(perPointTime.size)
}

enum class ServerState {
    LIVE, MANUAL, SHUTTING_DOWN, STACK, STARTUP, ABLATION
}

fun ServerState.toProto() = when (this) {
    ServerState.LIVE -> EnumServerState.SERVER_STATE_LIVE
    ServerState.MANUAL -> EnumServerState.SERVER_STATE_MANUAL
    ServerState.SHUTTING_DOWN -> EnumServerState.SERVER_STATE_SHUTTING_DOWN
    ServerState.STACK -> EnumServerState.SERVER_STATE_STACK
    ServerState.STARTUP -> EnumServerState.SERVER_STATE_STARTUP
    ServerState.ABLATION -> EnumServerState.SERVER_STATE_ABLATION
}

fun EnumServerState.toPoko() = when (this) {
    EnumServerState.SERVER_STATE_UNKNOWN -> throw IllegalArgumentException("Cant convert to ServerState")
    EnumServerState.SERVER_STATE_LIVE -> ServerState.LIVE
    EnumServerState.SERVER_STATE_MANUAL -> ServerState.MANUAL
    EnumServerState.SERVER_STATE_SHUTTING_DOWN -> ServerState.SHUTTING_DOWN
    EnumServerState.SERVER_STATE_STACK -> ServerState.STACK
    EnumServerState.SERVER_STATE_STARTUP -> ServerState.STARTUP
    EnumServerState.SERVER_STATE_ABLATION -> ServerState.ABLATION
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

