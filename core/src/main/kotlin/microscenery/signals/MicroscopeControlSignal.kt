package microscenery.signals

import com.google.protobuf.ByteString
import org.joml.Vector2i
import org.joml.Vector3f


sealed class MicroscopeControlSignal {
    fun toBaseSignal() = BaseClientSignal.AppSpecific(this.toProto().toByteString())

    open fun toProto(): me.jancasus.microscenery.network.v3.MicroscopeControlSignal {
        val cs = me.jancasus.microscenery.network.v3.MicroscopeControlSignal.newBuilder()
        when (this) {
            is AcquireStack -> throw NotImplementedError("This case should be overwritten.")
            is Live -> cs.liveBuilder.build()
            is MoveStage -> throw NotImplementedError("This case should be overwritten.")
            Shutdown -> cs.shutdownBuilder.build()
            SnapImage -> cs.snapImageBuilder.build()
            Stop -> cs.stopBuilder.build()
            is AblationPoints -> throw NotImplementedError("This case should be overwritten.")
            is AblationShutter -> throw NotImplementedError("This case should be overwritten.")
            StartAcquisition -> cs.startAcquisitionBuilder.build()
            is DeviceSpecific -> throw NotImplementedError("This case should be overwritten.")
        }
        return cs.build()
    }

    object Live : MicroscopeControlSignal()
    object Shutdown : MicroscopeControlSignal()
    object SnapImage : MicroscopeControlSignal()
    object Stop : MicroscopeControlSignal()
    object StartAcquisition : MicroscopeControlSignal()

    data class MoveStage(val target: Vector3f) : MicroscopeControlSignal() {
        override fun toProto(): me.jancasus.microscenery.network.v3.MicroscopeControlSignal {
            val cs = me.jancasus.microscenery.network.v3.MicroscopeControlSignal.newBuilder()
            cs.moveStageBuilder.setTarget(this.target.toProto()).build()
            return cs.build()
        }
    }

    data class AcquireStack(
        val startPosition: Vector3f,
        val endPosition: Vector3f,
        val stepSize: Float,
        val live: Boolean = false,
        val roiStart: Vector2i = Vector2i(),
        val roiEnd: Vector2i = Vector2i(),
        val id: Int = -1
    ) : MicroscopeControlSignal() {
        override fun toProto(): me.jancasus.microscenery.network.v3.MicroscopeControlSignal {
            val cs = me.jancasus.microscenery.network.v3.MicroscopeControlSignal.newBuilder()
            val asb = cs.acquireStackBuilder
            asb.startPosition = this.startPosition.toProto()
            asb.endPosition = this.endPosition.toProto()
            asb.stepSize = this.stepSize
            asb.live = this.live
            asb.roiStart = this.roiStart.toProto()
            asb.roiEnd = this.roiEnd.toProto()
            asb.id = this.id
            asb.build()
            return cs.build()
        }
    }

    data class AblationPoints(val points: List<AblationPoint>) : MicroscopeControlSignal() {
        override fun toProto(): me.jancasus.microscenery.network.v3.MicroscopeControlSignal {
            val cs = me.jancasus.microscenery.network.v3.MicroscopeControlSignal.newBuilder()
            val b = cs.ablationPointsBuilder
            b.addAllPoints(points.map { it.toProto() })
            b.build()
            return cs.build()
        }
    }

    data class AblationShutter(val open: Boolean) : MicroscopeControlSignal() {
        override fun toProto(): me.jancasus.microscenery.network.v3.MicroscopeControlSignal {
            val cs = me.jancasus.microscenery.network.v3.MicroscopeControlSignal.newBuilder()
            val b = cs.ablationShutterBuilder
            b.open = open
            b.build()
            return cs.build()
        }
    }

    data class AblationPoint(
        val position: Vector3f = Vector3f(),
        val dwellTime: Long = 0,
        val laserOn: Boolean = false,
        val laserOff: Boolean = false,
        val laserPower: Float = 0f,
        val countMoveTime: Boolean = false
    ) {
        fun toProto(): me.jancasus.microscenery.network.v3.AblationPoint {
            val b = me.jancasus.microscenery.network.v3.AblationPoint.newBuilder()
            b.position = position.toProto()
            b.dwellTime = dwellTime
            b.laserOn = laserOn
            b.laserOff = laserOff
            b.laserPower = laserPower
            b.countMoveTime = countMoveTime
            return b.build()
        }
    }

    data class DeviceSpecific(val data: ByteArray) : MicroscopeControlSignal() {
        // autogenerated equas and hashcode because intellij said so
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as DeviceSpecific
            return data.contentEquals(other.data)
        }

        override fun hashCode(): Int {
            return data.contentHashCode()
        }

        override fun toProto(): me.jancasus.microscenery.network.v3.MicroscopeControlSignal {
            val cs = me.jancasus.microscenery.network.v3.MicroscopeControlSignal.newBuilder()
            val ds = cs.deviceSpecificBuilder
            ds.setData(ByteString.copyFrom(data))
            ds.build()
            return cs.build()
        }
    }

    companion object {
        fun BaseClientSignal.AppSpecific.toMicroscopeControlSignal() =
            me.jancasus.microscenery.network.v3.MicroscopeControlSignal.parseFrom(this.data).toPoko()

        fun me.jancasus.microscenery.network.v3.MicroscopeControlSignal.toPoko(): MicroscopeControlSignal =
            when (this.signalCase ?: throw IllegalArgumentException("Illegal payload")) {
                me.jancasus.microscenery.network.v3.MicroscopeControlSignal.SignalCase.SIGNAL_NOT_SET ->
                    throw IllegalArgumentException("Signal is not set in Client signal message")

                me.jancasus.microscenery.network.v3.MicroscopeControlSignal.SignalCase.LIVE ->
                    Live

                me.jancasus.microscenery.network.v3.MicroscopeControlSignal.SignalCase.MOVESTAGE ->
                    MoveStage(this.moveStage.target.toPoko())

                me.jancasus.microscenery.network.v3.MicroscopeControlSignal.SignalCase.SHUTDOWN ->
                    Shutdown

                me.jancasus.microscenery.network.v3.MicroscopeControlSignal.SignalCase.SNAPIMAGE ->
                    SnapImage

                me.jancasus.microscenery.network.v3.MicroscopeControlSignal.SignalCase.ACQUIRESTACK -> {
                    val ast = this.acquireStack
                    AcquireStack(
                        ast.startPosition.toPoko(),
                        ast.endPosition.toPoko(),
                        ast.stepSize,
                        ast.live,
                        ast.roiStart.toPoko(),
                        ast.roiEnd.toPoko(),
                        ast.id
                    )
                }

                me.jancasus.microscenery.network.v3.MicroscopeControlSignal.SignalCase.STOP -> Stop
                me.jancasus.microscenery.network.v3.MicroscopeControlSignal.SignalCase.ABLATIONPOINTS -> {
                    val points = this.ablationPoints.pointsList
                    AblationPoints(points.map {
                        AblationPoint(
                            it.position.toPoko(),
                            it.dwellTime,
                            it.laserOn,
                            it.laserOff,
                            it.laserPower,
                            it.countMoveTime
                        )
                    })
                }

                me.jancasus.microscenery.network.v3.MicroscopeControlSignal.SignalCase.ABLATIONSHUTTER ->
                    AblationShutter(this.ablationShutter.open)

                me.jancasus.microscenery.network.v3.MicroscopeControlSignal.SignalCase.STARTACQUISITION -> StartAcquisition
                me.jancasus.microscenery.network.v3.MicroscopeControlSignal.SignalCase.DEVICESPECIFIC -> {
                    DeviceSpecific(this.deviceSpecific.data.toByteArray())
                }
            }
    }
}