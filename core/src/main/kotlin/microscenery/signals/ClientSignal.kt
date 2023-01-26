package microscenery.signals

import org.joml.Vector2i
import org.joml.Vector3f


sealed class ClientSignal {

    open fun toProto(): me.jancasus.microscenery.network.v2.ClientSignal {
        val cs = me.jancasus.microscenery.network.v2.ClientSignal.newBuilder()
        when (this) {
            is AcquireStack -> throw NotImplementedError("This case should be overwritten.")
            ClientSignOn -> cs.clientSignOnBuilder.build()
            is Live -> cs.liveBuilder.build()
            is MoveStage -> throw NotImplementedError("This case should be overwritten.")
            Shutdown -> cs.shutdownBuilder.build()
            SnapImage -> cs.snapImageBuilder.build()
            Stop -> cs.stopBuilder.build()
            is AblationPoints -> throw NotImplementedError("This case should be overwritten.")
            is AblationShutter -> throw NotImplementedError("This case should be overwritten.")
        }
        return cs.build()
    }

    object Live : ClientSignal()
    object ClientSignOn : ClientSignal()
    object Shutdown : ClientSignal()
    object SnapImage : ClientSignal()
    object Stop : ClientSignal()

    data class MoveStage(val target: Vector3f) : ClientSignal() {
        override fun toProto(): me.jancasus.microscenery.network.v2.ClientSignal {
            val cs = me.jancasus.microscenery.network.v2.ClientSignal.newBuilder()
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
        val roiEnd: Vector2i = Vector2i()
    ) : ClientSignal() {
        override fun toProto(): me.jancasus.microscenery.network.v2.ClientSignal {
            val cs = me.jancasus.microscenery.network.v2.ClientSignal.newBuilder()
            val asb = cs.acquireStackBuilder
            asb.startPosition = this.startPosition.toProto()
            asb.endPosition = this.endPosition.toProto()
            asb.stepSize = this.stepSize
            asb.live = this.live
            asb.roiStart = this.roiStart.toProto()
            asb.roiEnd = this.roiEnd.toProto()
            asb.build()
            return cs.build()
        }
    }

    data class AblationPoints(val points: List<AblationPoint>):ClientSignal(){
        override fun toProto(): me.jancasus.microscenery.network.v2.ClientSignal {
            val cs = me.jancasus.microscenery.network.v2.ClientSignal.newBuilder()
            val b = cs.ablationPointsBuilder
            b.addAllPoints(points.map { it.toProto() })
            b.build()
            return cs.build()
        }
    }

    data class AblationShutter(val open: Boolean):ClientSignal(){
        override fun toProto(): me.jancasus.microscenery.network.v2.ClientSignal {
            val cs = me.jancasus.microscenery.network.v2.ClientSignal.newBuilder()
            val b = cs.ablationShutterBuilder
            b.open = open
            b.build()
            return cs.build()
        }
    }

    data class AblationPoint(
        val position: Vector3f,
        val dwellTime: Long  ,
        val laserOn: Boolean,
        val laserOff: Boolean,
        val laserPower: Float,
        val countMoveTime: Boolean
    ){
        fun toProto(): me.jancasus.microscenery.network.v2.AblationPoint{
            val b = me.jancasus.microscenery.network.v2.AblationPoint.newBuilder()
            b.position = position.toProto()
            b.dwellTime = dwellTime
            b.laserOn = laserOn
            b.laserOff = laserOff
            b.laserPower = laserPower
            b.countMoveTime = countMoveTime
            return b.build()
        }
    }

    companion object {
        fun me.jancasus.microscenery.network.v2.ClientSignal.toPoko(): ClientSignal =
            when (this.signalCase ?: throw IllegalArgumentException("Illegal payload")) {
                me.jancasus.microscenery.network.v2.ClientSignal.SignalCase.SIGNAL_NOT_SET ->
                    throw IllegalArgumentException("Signal is not set in Client signal message")
                me.jancasus.microscenery.network.v2.ClientSignal.SignalCase.LIVE ->
                    Live
                me.jancasus.microscenery.network.v2.ClientSignal.SignalCase.MOVESTAGE ->
                    MoveStage(this.moveStage.target.toPoko())
                me.jancasus.microscenery.network.v2.ClientSignal.SignalCase.CLIENTSIGNON ->
                    ClientSignOn
                me.jancasus.microscenery.network.v2.ClientSignal.SignalCase.SHUTDOWN ->
                    Shutdown
                me.jancasus.microscenery.network.v2.ClientSignal.SignalCase.SNAPIMAGE ->
                    SnapImage
                me.jancasus.microscenery.network.v2.ClientSignal.SignalCase.ACQUIRESTACK -> {
                    val ast = this.acquireStack
                    AcquireStack(
                        ast.startPosition.toPoko(),
                        ast.endPosition.toPoko(),
                        ast.stepSize,
                        ast.live,
                        ast.roiStart.toPoko(),
                        ast.roiEnd.toPoko()
                    )
                }
                me.jancasus.microscenery.network.v2.ClientSignal.SignalCase.STOP -> Stop
                me.jancasus.microscenery.network.v2.ClientSignal.SignalCase.ABLATIONPOINTS ->{
                    val points = this.ablationPoints.pointsList
                    AblationPoints(points.map{
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
                me.jancasus.microscenery.network.v2.ClientSignal.SignalCase.ABLATIONSHUTTER ->
                    AblationShutter(this.ablationShutter.open)
            }
    }
}