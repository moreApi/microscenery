package network

import com.google.protobuf.ByteString
import microscenery.lightSleepOnNull
import microscenery.network.ControlSignalsClient
import microscenery.network.ControlSignalsServer
import microscenery.signals.BaseClientSignal
import microscenery.signals.BaseServerSignal
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.zeromq.ZContext
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ControlSignalTransmissionTest {

    var ctx = ZContext()
    var lastClientBaseSignal: BaseClientSignal? = null
    var lastServerBaseSignal: BaseServerSignal? = null
    var server: ControlSignalsServer = ControlSignalsServer(ctx, 11543, listOf {
        lastClientBaseSignal = it
    })
    var client: ControlSignalsClient = ControlSignalsClient(ctx, 11543, "*", listOf {
        lastServerBaseSignal = it
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
    fun signOn() {
        lightSleepOnNull { lastClientBaseSignal }
        assertNotNull(lastClientBaseSignal as? BaseClientSignal.ClientSignOn)
    }

    @Test
    fun appSpecificClientToServer() {
        lightSleepOnNull { lastClientBaseSignal }
        assertNotNull(lastClientBaseSignal as? BaseClientSignal.ClientSignOn)

        val payload = "HelloWorld"
        server.sendSignal(BaseServerSignal.AppSpecific(ByteString.copyFrom(payload.toByteArray())))

        lastServerBaseSignal = null
        lightSleepOnNull { lastServerBaseSignal }
        val transmitted = lastServerBaseSignal as? BaseServerSignal.AppSpecific
        assertNotNull(transmitted)

        assertEquals(payload, transmitted.data.toStringUtf8())
    }

    @Test
    fun appSpecificServerToClient() {
        lightSleepOnNull { lastClientBaseSignal }
        assertNotNull(lastClientBaseSignal as? BaseClientSignal.ClientSignOn)

        val payload = "HelloWorld"
        client.sendSignal(BaseClientSignal.AppSpecific(ByteString.copyFrom(payload.toByteArray())))

        lastClientBaseSignal = null
        lightSleepOnNull { lastClientBaseSignal }
        val transmitted = lastClientBaseSignal as? BaseClientSignal.AppSpecific
        assertNotNull(transmitted)

        assertEquals(payload, transmitted.data.toStringUtf8())
    }
/*
    @Test
    fun manySignals() {

        lightSleepOnNull { lastClientBaseSignal }
        assertNotNull(lastClientBaseSignal is BaseClientSignal.ClientSignOn)
        //assert(lastSignalServer == null)

        var countServer = 0
        server.addListener {
            countServer++
            // just to have an answer signal
            thread {
                Thread.sleep(200)
                server.sendSignal(RemoteMicroscopeStatus(emptyList(), 0).toBaseSignal())
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

 */

}