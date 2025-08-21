package microscenery.signals

import com.google.protobuf.ByteString
import com.google.protobuf.util.Timestamps.fromMillis
import microscenery.signals.BaseServerSignal.DataAvailableSignal.Companion.toPoko
import microscenery.signals.BaseServerSignal.ServerHello.Companion.toPoko
import microscenery.signals.ImageMeta.Companion.toPoko
import org.joml.Vector2i
import org.joml.Vector3f
import org.withXR.network.v3.EnumNumericType
import org.withXR.network.v3.EnumServerType
import java.nio.ByteBuffer

sealed class BaseServerSignal {

    abstract fun toProto(): org.withXR.network.v3.BaseServerSignal

    data class AppSpecific(val data: ByteString): BaseServerSignal(){
        override fun toProto(): org.withXR.network.v3.BaseServerSignal {
            val b = org.withXR.network.v3.BaseServerSignal.newBuilder()
            val asb = b.appSpecificBuilder
            asb.setData(data)
            return b.build()
        }
    }

    sealed class DataAvailableSignal : BaseServerSignal() {
        override fun toProto(): org.withXR.network.v3.BaseServerSignal {
            val b = org.withXR.network.v3.BaseServerSignal.newBuilder()
            b.dataAvailableSignal = this.toProtoIntern()
            return b.build()
        }

        protected abstract fun toProtoIntern(): org.withXR.network.v3.DataAvailableSignal

        companion object {
            fun org.withXR.network.v3.DataAvailableSignal.toPoko() =
                when (this.availableDataCase ?: throw IllegalArgumentException("Illegal payload")) {
                    org.withXR.network.v3.DataAvailableSignal.AvailableDataCase.STACK -> {
                        val s = this.stack
                        Stack(
                            s.id,
                            s.from.toPoko(),
                            s.to.toPoko(),
                            s.slicesCount,
                            s.created.seconds * 1000 + s.created.nanos.div(1000),
                            s.imageMeta.toPoko()
                        )
                    }

                    org.withXR.network.v3.DataAvailableSignal.AvailableDataCase.SLICE -> {
                        val s = this.slice
                        Slice(
                            s.id,
                            s.created.seconds * 1000 + s.created.nanos.div(1000),
                            s.stagePos.toPoko(),
                            s.size,
                            if (s.relatedToId == -1) null else s.relatedToId to s.stackSliceIndex,
                            s.imageMeta.toPoko(),
                            null
                        )
                    }

                    org.withXR.network.v3.DataAvailableSignal.AvailableDataCase.AVAILABLEDATA_NOT_SET ->
                        throw IllegalArgumentException("${this.javaClass.simpleName} is not set")
                }
        }

    }

    /**
     * Self identification message that will be send to new clients.
     * @param serverName be careful with special characters. This variable is also the bonjour name which prefers plain letters.
     */
    data class ServerHello(
        val serverName: String,
        val serverType: ServerType,
        val subType: String
    ) : BaseServerSignal() {

        override fun toProto(): org.withXR.network.v3.BaseServerSignal {
            val b = org.withXR.network.v3.BaseServerSignal.newBuilder()
            val b2 = b.serverHelloBuilder
            b2.serverName = serverName
            b2.serverType = serverType.toProto()
            b2.subType = subType

            return b.build()
        }

        companion object{
            fun org.withXR.network.v3.ServerHello.toPoko() =
                ServerHello(this.serverName, this.serverType.toPoko(), this.subType)
        }
    }


    companion object {
        fun org.withXR.network.v3.BaseServerSignal.toPoko() =
            when (this.signalCase?: throw IllegalArgumentException("Illegal payload")) {
                org.withXR.network.v3.BaseServerSignal.SignalCase.SIGNAL_NOT_SET ->
                    throw IllegalArgumentException("Signal is not set in Client signal message")
                org.withXR.network.v3.BaseServerSignal.SignalCase.DATAAVAILABLESIGNAL ->
                    this.dataAvailableSignal.toPoko()
                org.withXR.network.v3.BaseServerSignal.SignalCase.APPSPECIFIC ->
                    AppSpecific(this.appSpecific.data)
                org.withXR.network.v3.BaseServerSignal.SignalCase.SERVERHELLO ->
                    this.serverHello.toPoko()
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
    val imageMeta: ImageMeta,
    val data: ByteBuffer?
) : BaseServerSignal.DataAvailableSignal() {
    override fun toProtoIntern(): org.withXR.network.v3.DataAvailableSignal {
        val microscopeSignal = org.withXR.network.v3.DataAvailableSignal.newBuilder()

        val s = microscopeSignal.sliceBuilder
        s.id = this.Id
        s.created = fromMillis(this.created)
        s.stagePos = this.stagePos.toProto()
        s.size = this.size
        s.relatedToId = this.stackIdAndSliceIndex?.first ?: -1
        s.stackSliceIndex = this.stackIdAndSliceIndex?.second ?: -1
        s.imageMeta = this.imageMeta.toProto()
        s.build()

        return microscopeSignal.build()
    }
}

data class Stack(
    val Id: Int,
    val from: Vector3f,
    val to: Vector3f,
    val slicesCount: Int,
    val created: Long,
    val imageMeta: ImageMeta
) : BaseServerSignal.DataAvailableSignal() {
    override fun toProtoIntern(): org.withXR.network.v3.DataAvailableSignal {
        val microscopeSignal = org.withXR.network.v3.DataAvailableSignal.newBuilder()

        val s = microscopeSignal.stackBuilder
        s.id = this.Id
        s.created = fromMillis(this.created)
        s.from = this.from.toProto()
        s.to = this.to.toProto()
        s.slicesCount = this.slicesCount
        s.relatedToId = -1
        s.imageMeta = this.imageMeta.toProto()
        s.build()

        return microscopeSignal.build()
    }
}

data class ImageMeta(
    val imageSize: Vector2i,
    val vertexDiameter: Float,
    val numericType: NumericType
) {

    fun toProto(): org.withXR.network.v3.ImageMeta {
        val builder = org.withXR.network.v3.ImageMeta.newBuilder()

        builder.imageSize = this.imageSize.toProto()
        builder.vertexDiameter = this.vertexDiameter
        builder.numericType = this.numericType.toProto()

        return builder.build()
    }

    companion object {
        val EMPTY = ImageMeta(Vector2i(), 1f, NumericType.INT16)


        fun org.withXR.network.v3.ImageMeta.toPoko(): ImageMeta {
            return ImageMeta(
                this.imageSize.toPoko(),
                this.vertexDiameter,
                this.numericType.toPoko()
            )
        }
    }
}


enum class NumericType(val bytes: Int) {
    INT8(1),
    INT16(2);

    companion object {
        fun fromNumBytes(bytes: Int) = entries.firstOrNull { it.bytes == bytes }
    }
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


enum class ServerType{
    OTHER, VIEWER, MICROSCOPE;

    fun toProto() = when(this) {
        OTHER -> EnumServerType.SERVER_TYPE_OTHER
        VIEWER -> EnumServerType.SERVER_TYPE_VIEWER
        MICROSCOPE -> EnumServerType.SERVER_TYPE_MICROSCOPE
    }
}

fun EnumServerType.toPoko() = when(this) {
    EnumServerType.SERVER_TYPE_UNKNOWN -> throw IllegalArgumentException("Cant convert to ServerType")
    EnumServerType.SERVER_TYPE_OTHER -> ServerType.OTHER
    EnumServerType.SERVER_TYPE_VIEWER -> ServerType.VIEWER
    EnumServerType.SERVER_TYPE_MICROSCOPE -> ServerType.MICROSCOPE
    EnumServerType.UNRECOGNIZED -> throw IllegalArgumentException("Cant convert to ServerType")
}