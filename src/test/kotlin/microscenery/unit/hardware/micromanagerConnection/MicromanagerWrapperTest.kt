package microscenery.unit.hardware.micromanagerConnection

import microscenery.MicroscenerySettings
import microscenery.hardware.MicroscopeHardware
import microscenery.hardware.micromanagerConnection.MMConnection
import microscenery.hardware.micromanagerConnection.MicromanagerWrapper
import microscenery.millisToNanos
import microscenery.pollForSignal
import microscenery.signals.*
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse

internal class MicromanagerWrapperTest {
    @Test
    fun outOfBoundsRequest() {

        MicroscenerySettings.set("Stage.minX", -100f)
        MicroscenerySettings.set("Stage.minY", -100f)
        MicroscenerySettings.set("Stage.minZ", -100f)
        MicroscenerySettings.set("Stage.maxX", 100f)
        MicroscenerySettings.set("Stage.maxY", 100f)
        MicroscenerySettings.set("Stage.maxZ", 100f)

        val mmConnection = Mockito.mock(MMConnection::class.java)

        whenever(mmConnection.width).thenReturn(200)
        whenever(mmConnection.height).thenReturn(200)
        whenever(mmConnection.stagePosition).thenReturn(Vector3f())


        val wrapper = MicromanagerWrapper(mmConnection, disableStagePosUpdates = true)
        wrapper.output.pollForSignal<MicroscopeStatus>()

        //start testing
        wrapper.stagePosition = (Vector3f(-400f))
        wrapper.output.pollForSignal<MicroscopeStatus>()

        wrapper.stagePosition = (Vector3f(400f))
        wrapper.output.pollForSignal<MicroscopeStatus>()
        verify(mmConnection).moveStage(Vector3f(-100f), true)
        verify(mmConnection).moveStage(Vector3f(100f), true)

    }

    @Test
    fun rangeCheckAtStartup() {

        val mmConnection = Mockito.mock(MMConnection::class.java)

        whenever(mmConnection.width).thenReturn(200)
        whenever(mmConnection.height).thenReturn(200)
        whenever(mmConnection.stagePosition).thenReturn(Vector3f(-4000f))

        assertThrows<IllegalStateException> {
            val wrapper = spy(MicromanagerWrapper(mmConnection) as MicroscopeHardware)
            wrapper.output.pollForSignal<MicroscopeStatus>()
        }

        //no error is happening
        assertEquals(Vector3f(-4000f), mmConnection.stagePosition)
        verify(mmConnection, never()).moveStage(any(), any())
    }

    @Test
    fun minimalStartup() {

        val mmConnection = Mockito.mock(MMConnection::class.java)

        whenever(mmConnection.width).thenReturn(200)
        whenever(mmConnection.height).thenReturn(200)
        whenever(mmConnection.stagePosition).thenReturn(Vector3f(0f))


        val wrapper = MicromanagerWrapper(mmConnection)
        //start testing
        assert(wrapper.output.pollForSignal<HardwareDimensions>(ignoreNotFitting = false))
        assert(wrapper.output.pollForSignal<MicroscopeStatus>(ignoreNotFitting = false))
        assertFalse(wrapper.output.pollForSignal<MicroscopeSignal>(ignoreNotFitting = true))
    }

    /**
     * Timed test to make sure that the ablation starts and stops fast after a stop command.
     */
    @Test
    fun ablationAndStop() {
        val mmConnection = Mockito.mock(MMConnection::class.java)
        whenever(mmConnection.width).thenReturn(200)
        whenever(mmConnection.height).thenReturn(200)
        whenever(mmConnection.stagePosition).thenReturn(Vector3f(0f))
        var laserPower = 0f
        whenever(mmConnection.laserPower(0.5f)).then{ laserPower = 0.5f; 5f }
        whenever(mmConnection.laserPower(0f)).then{ laserPower = 0f; 5f }
        val wrapper = MicromanagerWrapper(mmConnection)
        wrapper.output.pollForSignal<MicroscopeStatus>() //wait for start up to finish

        wrapper.ablatePoints(ClientSignal.AblationPoints(
            (0..500).map { ClientSignal.AblationPoint(Vector3f(), 50L.millisToNanos(), true, false, 0.5f, false) }
        ))
        assert(wrapper.output.pollForSignal<MicroscopeStatus>(ignoreNotFitting = true) { it.state == ServerState.ABLATION })
        Thread.sleep(200)
        assertEquals(0.5f, laserPower)
        wrapper.stop()

        Thread.sleep(200)
        assertEquals(0f, laserPower)
        assertEquals(ServerState.MANUAL,wrapper.status().state)

        val order = inOrder(mmConnection)
        order.verify(mmConnection).ablationShutter(true,true)
        order.verify(mmConnection).ablationShutter(false,true)
    }
}
