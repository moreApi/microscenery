package microscenery.unit.network

import microscenery.lightSleepOnCondition
import microscenery.lightSleepOnNull
import microscenery.network.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.zeromq.ZContext
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ControlSignalTransmissionTest {

    var ctx = ZContext()

    @AfterEach
    fun reset() {
        ctx.linger = 0
        ctx.destroy()
        ctx = ZContext()
    }

    @Test
    fun shutdownServer() {
        val client = ControlSignalsServer(ctx)

        client.sendSignal(ServerSignal.ServerStatus(ServerState.SHUTTING_DOWN, listOf(), 0, HardwareDimensions.EMPTY))

        lightSleepOnCondition { !client.running }
        assert(!client.running)
    }


    @Test
    fun integration() {
        var lastSignalServer: ServerSignal? = null
        var lastSignalClient: ClientSignal? = null
        val server = ControlSignalsServer(ctx, 11543, listOf {
            lastSignalClient = it
        })
        val client = ControlSignalsClient(ctx, 11543, "*", listOf {
            lastSignalServer = it
        })

        lightSleepOnNull { lastSignalClient }
        assertNotNull(lastSignalClient is ClientSignal.ClientSignOn)
        assert(lastSignalServer == null)

        val s1 = ServerSignal.ServerStatus.EMPTY.copy(ServerState.MANUAL)

        server.sendSignal(s1)
        lightSleepOnNull { lastSignalServer }
        val s1trans = lastSignalServer as? ServerSignal.ServerStatus
        assertNotNull(s1trans)
        assertEquals(ServerState.MANUAL, s1trans.state)

//        lastSignalClient = null
//        client.sendSignal(ClientSignal.StartImaging)
//        lightSleepOnNull { lastSignalClient }
//        assertNotNull(lastSignalClient as ClientSignal.StartImaging)
//
//        val s2 = ServerSignal.Status(Vector3i(1, 2, 3), ServerState.Imaging, listOf(22, 33))
//        lastSignalServer = null
//        server.sendSignal(s2)
//        lightSleepOnNull { lastSignalServer }
//        val s2trans = lastSignalServer as? ServerSignal.Status
//        assertNotNull(s2trans)
//        assert(s2trans.state == ServerState.Imaging)
//        assert(s2trans.imageSize == s2.imageSize)
//        assert(s2trans.dataPorts.containsAll(s2.dataPorts))
//
//        val s3 = ServerSignal.Status(Vector3i(1, 2, 3), ServerState.ShuttingDown, listOf(22, 33))
//        lastSignalServer = null
//        server.sendSignal(s3)
//        lightSleepOnNull { lastSignalServer }
//        val s3trans = lastSignalServer as? ServerSignal.Status
//        assertNotNull(s3trans)
//        assert(s3trans.state == ServerState.ShuttingDown)
//
//        server.thread.join(5000)
//        assert(!server.thread.isAlive)
//        client.thread.join(5000)
//        assert(!client.thread.isAlive)

    }
}