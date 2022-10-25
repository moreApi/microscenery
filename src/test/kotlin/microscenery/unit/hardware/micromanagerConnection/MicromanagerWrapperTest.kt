package microscenery.unit.hardware.micromanagerConnection

import microscenery.MicroscenerySettings
import microscenery.hardware.micromanagerConnection.MMConnection
import microscenery.hardware.micromanagerConnection.MicromanagerWrapper
import microscenery.pollForSignal
import microscenery.signals.MicroscopeStatus
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

internal class MicromanagerWrapperTest{
    @Test
    fun outOfBoundsRequest(){

        MicroscenerySettings.set("Stage.minX",-100f)
        MicroscenerySettings.set("Stage.minY",-100f)
        MicroscenerySettings.set("Stage.minZ",-100f)
        MicroscenerySettings.set("Stage.maxX",100f)
        MicroscenerySettings.set("Stage.maxY",100f)
        MicroscenerySettings.set("Stage.maxZ",100f)

        val mmConnection = Mockito.mock(MMConnection::class.java)

        whenever(mmConnection.width).thenReturn(200)
        whenever(mmConnection.height).thenReturn(200)
        whenever(mmConnection.stagePosition).thenReturn(Vector3f())


        val wrapper = MicromanagerWrapper(mmConnection)
        wrapper.output.pollForSignal<MicroscopeStatus>()

        //start testing
        wrapper.stagePosition = (Vector3f(-400f))
        wrapper.output.pollForSignal<MicroscopeStatus>()
        verify(mmConnection).moveStage(Vector3f(-100f),true)

        wrapper.stagePosition = (Vector3f(400f))
        wrapper.output.pollForSignal<MicroscopeStatus>()
        verify(mmConnection).moveStage(Vector3f(100f),true)

    }
}