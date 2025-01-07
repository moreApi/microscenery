package microscenery.signals

import microscenery.signals.MicroscopeSignal.Companion.toPoko


sealed class RemoteMicroscopeSignal {
    fun toBaseSignal() = BaseServerSignal.AppSpecific(this.toProto().toByteString())

    abstract fun toProto(): me.jancasus.microscenery.network.v3.RemoteMicroscopeSignal

    companion object {
        fun BaseServerSignal.AppSpecific.toRemoteMicroscopeSignal() =
            me.jancasus.microscenery.network.v3.RemoteMicroscopeSignal.parseFrom(this.data).toPoko()

        fun me.jancasus.microscenery.network.v3.RemoteMicroscopeSignal.toPoko() =
            when (this.signalCase ?: throw IllegalArgumentException("Illegal payload")) {
                me.jancasus.microscenery.network.v3.RemoteMicroscopeSignal.SignalCase.STATUS -> {
                    val ss = this.status
                    RemoteMicroscopeStatus(
                        ss.dataPortsList,
                        ss.connectedClients
                    )
                }

                me.jancasus.microscenery.network.v3.RemoteMicroscopeSignal.SignalCase.MICROSCOPESIGNAL ->
                    ActualMicroscopeSignal(this.microscopeSignal.toPoko())

                me.jancasus.microscenery.network.v3.RemoteMicroscopeSignal.SignalCase.SIGNAL_NOT_SET ->
                    throw IllegalArgumentException("Signal is not set in Server signal message")
            }
    }
}

data class RemoteMicroscopeStatus(
    val dataPorts: List<Int>,
    val connectedClients: Int
) : RemoteMicroscopeSignal() {
    override fun toProto(): me.jancasus.microscenery.network.v3.RemoteMicroscopeSignal {
        val microscopeSignal = me.jancasus.microscenery.network.v3.RemoteMicroscopeSignal.newBuilder()

        val rmStatus = microscopeSignal.statusBuilder
        this.dataPorts.forEach { rmStatus.addDataPorts(it) }
        rmStatus.connectedClients = this.connectedClients
        rmStatus.build()

        return microscopeSignal.build()
    }
}

data class ActualMicroscopeSignal(val signal: MicroscopeSignal) : RemoteMicroscopeSignal() {
    override fun toProto(): me.jancasus.microscenery.network.v3.RemoteMicroscopeSignal {
        val microscopeSignal = me.jancasus.microscenery.network.v3.RemoteMicroscopeSignal.newBuilder()
        microscopeSignal.microscopeSignal = signal.toProto()
        return microscopeSignal.build()
    }
}
