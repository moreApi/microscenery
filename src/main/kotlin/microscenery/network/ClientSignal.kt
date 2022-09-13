package microscenery.network

import org.joml.Vector2i
import org.joml.Vector3f


sealed class ClientSignal {

    open fun toProto(): me.jancasus.microscenery.network.v2.ClientSignal {
        val cs = me.jancasus.microscenery.network.v2.ClientSignal.newBuilder()
        when(this){
            is AcquireStack -> throw NotImplementedError("This case should be overwritten.")
            ClientSignOn -> cs.clientSignOnBuilder.build()
            is Live -> throw NotImplementedError("This case should be overwritten.")
            is MoveStage -> throw NotImplementedError("This case should be overwritten.")
            Shutdown -> cs.shutdownBuilder.build()
            SnapImage -> cs.snapImageBuilder.build()
            Stop -> cs.stopBuilder.build()
        }
        return cs.build()
    }

    data class Live(val value: Boolean) : ClientSignal(){
        override fun toProto(): me.jancasus.microscenery.network.v2.ClientSignal {
            val cs = me.jancasus.microscenery.network.v2.ClientSignal.newBuilder()
            cs.liveBuilder.setValue(this.value).build()
            return cs.build()
        }
    }

    data class MoveStage(val target: Vector3f) : ClientSignal() {
        override fun toProto(): me.jancasus.microscenery.network.v2.ClientSignal {
            val cs = me.jancasus.microscenery.network.v2.ClientSignal.newBuilder()
            cs.moveStageBuilder.setTarget(this.target.toProto()).build()
            return cs.build()
        }
    }

    object ClientSignOn : ClientSignal()
    object Shutdown : ClientSignal()
    object SnapImage : ClientSignal()

    data class AcquireStack(
        val stageStartPosition: Vector3f,
        val  endZPosition: Float,
        val  stepSize: Float,
        val  live: Boolean,
        val  roiStart: Vector2i,
        val  roiEnd : Vector2i
    ) : ClientSignal() {
        override fun toProto(): me.jancasus.microscenery.network.v2.ClientSignal {
            val cs = me.jancasus.microscenery.network.v2.ClientSignal.newBuilder()
            val asb = cs.acquireStackBuilder
            asb.stageStartPosition = this.stageStartPosition.toProto()
            asb.endZPosition = this.endZPosition
            asb.stepSize = this.stepSize
            asb.live = this.live
            asb.roiStart = this.roiStart.toProto()
            asb.roiEnd = this.roiEnd.toProto()
            asb.build()
            return cs.build()
        }
    }

    object Stop: ClientSignal()

    companion object {
        fun me.jancasus.microscenery.network.v2.ClientSignal.toPoko(): ClientSignal =
            when (this.signalCase ?: throw IllegalArgumentException("Illegal payload")) {
                me.jancasus.microscenery.network.v2.ClientSignal.SignalCase.SIGNAL_NOT_SET ->
                    throw IllegalArgumentException("Signal is not set in Client signal message")
                me.jancasus.microscenery.network.v2.ClientSignal.SignalCase.LIVE ->
                    Live(this.live.value)
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
                        ast.stageStartPosition.toPoko(),
                        ast.endZPosition,
                        ast.stepSize,
                        ast.live,
                        ast.roiStart.toPoko(),
                        ast.roiEnd.toPoko()
                    )
                }
                me.jancasus.microscenery.network.v2.ClientSignal.SignalCase.STOP -> Stop
            }
    }
}