package microscenery.hardware.micromanagerConnection

import me.jancasus.microscenery.network.v3.MicroManagerSignal
import microscenery.hardware.MicroscopeHardware
import microscenery.signals.toProto
import org.joml.Vector3f

object MicroManagerUtil {
    fun addPositionToPositionList(microscopeHardware: MicroscopeHardware, label: String, position: Vector3f) {
        val mmSignalBuilder = MicroManagerSignal.newBuilder()
        val aptplBuilder = mmSignalBuilder.addToPositionListBuilder
        aptplBuilder.label = label
        aptplBuilder.pos = position.toProto()
        aptplBuilder.build()
        val protoSignal = mmSignalBuilder.build()

        microscopeHardware.deviceSpecificCommands(protoSignal.toByteArray())
    }
}