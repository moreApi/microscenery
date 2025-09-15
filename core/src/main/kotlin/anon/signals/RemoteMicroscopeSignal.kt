package anon.signals

import anon.signals.MicroscopeSignal.Companion.toPoko


sealed class RemoteMicroscopeSignal {
    fun toBaseSignal() = BaseServerSignal.AppSpecific(this.toProto().toByteString())

    abstract fun toProto(): org.withXR.network.v3.microscopeApi.RemoteMicroscopeSignal

    companion object {
        fun BaseServerSignal.AppSpecific.toRemoteMicroscopeSignal() =
            org.withXR.network.v3.microscopeApi.RemoteMicroscopeSignal.parseFrom(this.data).toPoko()

        fun org.withXR.network.v3.microscopeApi.RemoteMicroscopeSignal.toPoko() =
            when (this.signalCase ?: throw IllegalArgumentException("Illegal payload")) {
                org.withXR.network.v3.microscopeApi.RemoteMicroscopeSignal.SignalCase.STATUS -> {
                    val ss = this.status
                    RemoteMicroscopeStatus(
                        ss.dataPortsList,
                        ss.connectedClients
                    )
                }

                org.withXR.network.v3.microscopeApi.RemoteMicroscopeSignal.SignalCase.MICROSCOPESIGNAL ->
                    ActualMicroscopeSignal(this.microscopeSignal.toPoko())

                org.withXR.network.v3.microscopeApi.RemoteMicroscopeSignal.SignalCase.SIGNAL_NOT_SET ->
                    throw IllegalArgumentException("Signal is not set in Server signal message")
            }
    }
}

data class RemoteMicroscopeStatus(
    val dataPorts: List<Int>,
    val connectedClients: Int
) : RemoteMicroscopeSignal() {
    override fun toProto(): org.withXR.network.v3.microscopeApi.RemoteMicroscopeSignal {
        val microscopeSignal = org.withXR.network.v3.microscopeApi.RemoteMicroscopeSignal.newBuilder()

        val rmStatus = microscopeSignal.statusBuilder
        this.dataPorts.forEach { rmStatus.addDataPorts(it) }
        rmStatus.connectedClients = this.connectedClients
        rmStatus.build()

        return microscopeSignal.build()
    }
}

data class ActualMicroscopeSignal(val signal: MicroscopeSignal) : RemoteMicroscopeSignal() {
    override fun toProto(): org.withXR.network.v3.microscopeApi.RemoteMicroscopeSignal {
        val microscopeSignal = org.withXR.network.v3.microscopeApi.RemoteMicroscopeSignal.newBuilder()
        microscopeSignal.microscopeSignal = signal.toProto()
        return microscopeSignal.build()
    }
}
