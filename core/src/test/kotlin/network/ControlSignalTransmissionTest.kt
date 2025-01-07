package network

import microscenery.lightSleepOnCondition
import microscenery.lightSleepOnNull
import microscenery.network.ControlSignalsClient
import microscenery.network.ControlSignalsServer
import microscenery.signals.*
import microscenery.signals.MicroscopeControlSignal.Companion.toMicroscopeControlSignal
import microscenery.signals.RemoteMicroscopeSignal.Companion.toRemoteMicroscopeSignal
import org.joml.Vector2i
import org.joml.Vector3f
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.zeromq.ZContext
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ControlSignalTransmissionTest {

    var ctx = ZContext()
    var lastSignalServer: RemoteMicroscopeSignal? = null
    var lastSignalClient: MicroscopeControlSignal? = null
    var lastClientBaseSignal: BaseClientSignal? = null
    var lastServerBaseSignal: BaseServerSignal? = null
    var server: ControlSignalsServer = ControlSignalsServer(ctx, 11543, listOf {
        lastClientBaseSignal = it
        if (it is BaseClientSignal.AppSpecific)
            lastSignalClient = it.toMicroscopeControlSignal()
    })
    var client: ControlSignalsClient = ControlSignalsClient(ctx, 11543, "*", listOf {
        lastServerBaseSignal = it
        if (it is BaseServerSignal.AppSpecific)
            lastSignalServer = it.toRemoteMicroscopeSignal()
    })

    @AfterEach
    fun reset() {
        val serverThread = server.close()
        client.close().join()
        serverThread.join()

        ctx.linger = 0
        ctx.destroy()
        ctx = ZContext()
    }

    @Test
    fun shutdownServer() {
        server.sendSignal(
            ActualMicroscopeSignal(
                MicroscopeStatus(ServerState.SHUTTING_DOWN, Vector3f(), false)
            )
        )

        lightSleepOnCondition { !server.running }
        assert(!server.running)
    }

    @Test
    fun transmittingSnapCommand() {
        lightSleepOnNull { lastClientBaseSignal }
        assertNotNull(lastClientBaseSignal is BaseClientSignal.ClientSignOn)
        assert(lastSignalServer == null)

        lastSignalClient = null
        client.sendSignal(MicroscopeControlSignal.SnapImage.toBaseSignal())

        lightSleepOnNull { lastSignalClient }
        assertNotNull(lastSignalClient)
        assert(lastSignalClient is MicroscopeControlSignal.SnapImage)
    }

    @Test
    fun transmittingValues() {

        lightSleepOnNull { lastClientBaseSignal }
        assertNotNull(lastClientBaseSignal is BaseClientSignal.ClientSignOn)
        assert(lastSignalServer == null)

        val outHwd = HardwareDimensions(
            Vector3f(1f, 2f, 3f), Vector3f(2f), ImageMeta(Vector2i(20), 0.4f, NumericType.INT16)
        )
        server.sendSignal(ActualMicroscopeSignal(outHwd))

        lightSleepOnNull { lastSignalServer }
        val inSignal = lastSignalServer as? ActualMicroscopeSignal
        assertNotNull(inSignal)
        val inHwd = (inSignal.signal) as? HardwareDimensions
        assertNotNull(inHwd)
        assert(outHwd !== inHwd) // check that is not simply the same object
        assertEquals(outHwd.stageMax, inHwd.stageMax)
        assertEquals(outHwd.stageMin, inHwd.stageMin)
        assertEquals(outHwd.numericType, inHwd.numericType)

    }

    @Test
    fun transmittingState() {
        lightSleepOnNull { lastClientBaseSignal }
        assertNotNull(lastClientBaseSignal is BaseClientSignal.ClientSignOn)
        assert(lastSignalServer == null)

        val s1 = MicroscopeStatus(ServerState.MANUAL, Vector3f(), false)
        server.sendSignal(ActualMicroscopeSignal(s1))
        lightSleepOnNull { lastSignalServer }

        val returnSignal = lastSignalServer as? ActualMicroscopeSignal
        assertNotNull(returnSignal)
        val s1trans = returnSignal.signal as? MicroscopeStatus
        assertNotNull(s1trans)
        assertEquals(ServerState.MANUAL, s1trans.state)
    }

    @Test
    fun transmittingCommand() {
        lightSleepOnNull { lastClientBaseSignal }
        assertNotNull(lastClientBaseSignal is BaseClientSignal.ClientSignOn)
        assert(lastSignalServer == null)

        lastSignalClient = null
        client.sendSignal(MicroscopeControlSignal.SnapImage.toBaseSignal())
        lightSleepOnNull { lastSignalClient }
        assertNotNull(lastSignalClient as MicroscopeControlSignal.SnapImage)
    }

    @Test
    fun manySignals() {

        lightSleepOnNull { lastClientBaseSignal }
        assertNotNull(lastClientBaseSignal is BaseClientSignal.ClientSignOn)
        assert(lastSignalServer == null)

        var countServer = 0
        server.addListener {
            countServer++
            // just to have an answer signal
            thread {
                Thread.sleep(200)
                server.sendSignal(RemoteMicroscopeStatus(emptyList(), 0))
            }
        }

        var countClient = 0
        client.addListener { countClient++ }

        for (x in 1..2000) {
            assert(client.sendSignal(MicroscopeControlSignal.MoveStage(Vector3f(x.toFloat())).toBaseSignal()))
        }
        Thread.sleep(5000)

        assertEquals(2000, countServer)
        assertEquals(2000, countClient)
    }

}