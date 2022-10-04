package microscenery.unit.network

import microscenery.lightSleepOnCondition
import microscenery.lightSleepOnNull
import microscenery.network.*
import org.joml.Vector2i
import org.joml.Vector3f
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
        val server = ControlSignalsServer(ctx)

        server.sendSignal(ServerSignal.ServerStatus(ServerState.SHUTTING_DOWN, listOf(), 0, HardwareDimensions.EMPTY))

        lightSleepOnCondition { !server.running }
        assert(!server.running)
    }

    @Test
    fun transmittingSnapCommand() {

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

        lastSignalClient = null
        client.sendSignal(ClientSignal.SnapImage)

        lightSleepOnNull { lastSignalClient }
        assertNotNull(lastSignalClient)
        assert(lastSignalClient is ClientSignal.SnapImage)

        val serverThread = server.close()
        client.close().join()
        serverThread.join()
    }

    @Test
    fun transmittingValues() {

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

        val outStatus = ServerSignal.ServerStatus(
            ServerState.MANUAL,
            listOf(1, 2),
            3,
            HardwareDimensions(
                Vector3f(1f, 2f, 3f),
                Vector3f(2f),
                Vector2i(20),
                Vector3f(0.4f),
                NumericType.INT16
            )
        )
        server.sendSignal(outStatus)
        lightSleepOnNull { lastSignalServer }
        val inStatus = lastSignalServer as? ServerSignal.ServerStatus
        assertNotNull(inStatus)
        assert(outStatus !== inStatus) // check that is not simply the same object
        assertEquals(outStatus.state, inStatus.state)
        assertEquals(outStatus.connectedClients, inStatus.connectedClients)
        assert(inStatus.dataPorts.containsAll(outStatus.dataPorts))
        val outHwd = outStatus.hwDimensions
        val inHwd = inStatus.hwDimensions
        assert(outHwd !== inHwd) // check that is not simply the same object
        assertEquals(outHwd.stageMax, inHwd.stageMax)
        assertEquals(outHwd.stageMin, inHwd.stageMin)
        assertEquals(outHwd.numericType, inHwd.numericType)

        val serverThread = server.close()
        client.close().join()
        serverThread.join()
    }

    @Test
    fun transmittingState() {
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

        lastSignalClient = null
        client.sendSignal(ClientSignal.SnapImage)
        lightSleepOnNull { lastSignalClient }
        assertNotNull(lastSignalClient as ClientSignal.SnapImage)

        val serverThread = server.close()
        client.close().join()
        serverThread.join()

    }
}