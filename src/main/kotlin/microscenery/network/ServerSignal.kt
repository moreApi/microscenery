package microscenery.network

import com.google.protobuf.util.Timestamps.fromMillis
import me.jancasus.microscenery.network.v2.*
import microscenery.network.HardwareDimensions.Companion.toPoko
import org.joml.Vector2i
import org.joml.Vector3f
import org.joml.Vector3i
import java.lang.System.currentTimeMillis


sealed class ServerSignal {

    abstract fun toProto(): me.jancasus.microscenery.network.v2.ServerSignal

    data class ServerStatus(
        val state: ServerState,
        val dataPorts: List<Int>,
        val connectedClients: Int,
        val hwDimensions: HardwareDimensions
    ):ServerSignal() {
        override fun toProto(): me.jancasus.microscenery.network.v2.ServerSignal {
            val serverSignal = me.jancasus.microscenery.network.v2.ServerSignal.newBuilder()

            val serverStatus = serverSignal.serverStatusBuilder
            serverStatus.state = this.state.toProto()
            this.dataPorts.forEach { serverStatus.addDataPorts(it) }
            serverStatus.connectedClients = this.connectedClients
            serverStatus.hwDimensions = this.hwDimensions.toProto()
            serverStatus.build()

            return serverSignal.build()
        }
    }
    data class Slice(val Id: Int,
                     val  created: Long,
                     val  stagePos:Vector3f,
                     val  size: Int,
                     val stackId: Int?
    ) : ServerSignal() {
        override fun toProto(): me.jancasus.microscenery.network.v2.ServerSignal {
            val serverSignal = me.jancasus.microscenery.network.v2.ServerSignal.newBuilder()

            val s = serverSignal.sliceBuilder
            s.id = this.Id
            s.created = fromMillis(this.created)
            s.stagePos = this.stagePos.toProto()
            s.size = this.size
            s.stackId = this.stackId ?: -1
            s.build()

            return serverSignal.build()
        }
    }

    data class Stack(val Id: Int) : ServerSignal() {
        override fun toProto(): me.jancasus.microscenery.network.v2.ServerSignal {
            TODO("Stack Not yet implemented")
        }
    }

    companion object {
        fun me.jancasus.microscenery.network.v2.ServerSignal.toPoko() =
            when (this.signalCase ?: throw IllegalArgumentException("Illegal payload")){
                me.jancasus.microscenery.network.v2.ServerSignal.SignalCase.SERVERSTATUS -> {
                    val ss = this.serverStatus
                    ServerSignal.ServerStatus(
                        ss.state.toPoko(),
                        ss.dataPortsList,
                        ss.connectedClients,
                        ss.hwDimensions.toPoko())
                }
                me.jancasus.microscenery.network.v2.ServerSignal.SignalCase.STACK -> {
                    TODO("stack")
                }
                me.jancasus.microscenery.network.v2.ServerSignal.SignalCase.SLICE -> {
                    val s = this.slice
                    ServerSignal.Slice(
                        s.id,
                        s.created.seconds*1000 + s.created.nanos.div(1000),
                        s.stagePos.toPoko(),
                        s.size,
                        if (s.stackId == -1) null else s.stackId
                    )
                }
                me.jancasus.microscenery.network.v2.ServerSignal.SignalCase.SIGNAL_NOT_SET ->
                    throw IllegalArgumentException("Signal is not set in Server signal message")
            }
    }


}

enum class ServerState {
    LIVE, MANUAL, SHUTTING_DOWN, STACK, STARTUP,
}
fun ServerState.toProto() = when(this){
    ServerState.LIVE -> EnumServerState.SERVER_STATE_LIVE
    ServerState.MANUAL -> EnumServerState.SERVER_STATE_MANUAL
    ServerState.SHUTTING_DOWN -> EnumServerState.SERVER_STATE_SHUTTING_DOWN
    ServerState.STACK -> EnumServerState.SERVER_STATE_STACK
    ServerState.STARTUP -> EnumServerState.SERVER_STATE_STARTUP
}
fun EnumServerState.toPoko() = when(this){
    EnumServerState.SERVER_STATE_UNKNOWN -> throw IllegalArgumentException("Cant convert to ServerState")
    EnumServerState.SERVER_STATE_LIVE -> ServerState.LIVE
    EnumServerState.SERVER_STATE_MANUAL ->  ServerState.MANUAL
    EnumServerState.SERVER_STATE_SHUTTING_DOWN ->  ServerState.SHUTTING_DOWN
    EnumServerState.SERVER_STATE_STACK ->  ServerState.STACK
    EnumServerState.SERVER_STATE_STARTUP ->  ServerState.STARTUP
    EnumServerState.UNRECOGNIZED -> throw IllegalArgumentException("Cant convert to ServerState")
}


data class HardwareDimensions(
    val stageMin: Vector3f,
    val stageMax: Vector3f,
    val imageSize: Vector2i,
    val vertexSize: Vector3f,
    val numericType: NumericType
) {
    fun toProto(): me.jancasus.microscenery.network.v2.HardwareDimensions {
        val hd =me.jancasus.microscenery.network.v2.HardwareDimensions.newBuilder()

        hd.stageMin = this.stageMin.toProto()
        hd.stageMax = this.stageMax.toProto()
        hd.imageSize = this.imageSize.toProto()
        hd.vertexSize = this.vertexSize.toProto()
        hd.numericType = this.numericType.toProto()

        return hd.build()
    }

    val byteSize
        get() = imageSize.x * imageSize.y * numericType.bytes

    companion object {
        fun me.jancasus.microscenery.network.v2.HardwareDimensions.toPoko() =
            HardwareDimensions(
                this.stageMin.toPoko(),
                this.stageMax.toPoko(),
                this.imageSize.toPoko(),
                this.vertexSize.toPoko(),
                this.numericType.toPoko()
            )
    }
}

enum class NumericType(val bytes: Int) {
    INT16(2)
}
fun NumericType.toProto() = when (this) {
    NumericType.INT16 -> EnumNumericType.VALUE_NUMERIC_INT16
}
fun EnumNumericType.toPoko() = when (this){
    EnumNumericType.VALUE_NUMERIC_UNKNOWN -> throw IllegalArgumentException("Cant convert to NumericType")
    EnumNumericType.VALUE_NUMERIC_INT16 -> NumericType.INT16
    EnumNumericType.UNRECOGNIZED -> throw IllegalArgumentException("Cant convert to NumericType")
}

