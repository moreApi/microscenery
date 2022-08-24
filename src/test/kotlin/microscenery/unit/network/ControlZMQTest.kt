package microscenery.unit.network

import microscenery.lightSleepOn
import microscenery.example.network.zContext
import microscenery.network.*
import org.joml.Vector3i
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.zeromq.ZContext
import kotlin.test.assertNotNull

class ControlZMQTest {

    var ctx = ZContext()

    @AfterEach
    fun reset() {
        ctx.linger =  0
        ctx.destroy()
        ctx = ZContext()
        ctx.linger =  0
    }

    @Test
    fun integration(){
        var lastSignalServer : ServerSignal? = null
        var lastSignalClient : ClientSignal? = null
        val server = ControlZMQServer(zContext, 11543, listOf {
            lastSignalClient = it
        })
        val client = ControlZMQClient(zContext,11543,"*", listOf {
            lastSignalServer = it
        })

        lightSleepOn { lastSignalClient }
        assert(lastSignalClient is ClientSignal.ClientSignOn)
        assert(lastSignalServer == null)

        val s1 = ServerSignal.Status(Vector3i(0),ServerState.Paused, emptyList())
        server.sendSignal(s1)
        lightSleepOn { lastSignalServer }
        val s1trans = lastSignalServer as? ServerSignal.Status
        assertNotNull(s1trans)
        assert(s1trans.state == ServerState.Paused )
        assert(s1trans.imageSize == s1.imageSize)

        lastSignalClient = null
        client.sendSignal(ClientSignal.StartImaging)
        lightSleepOn { lastSignalClient }
        assertNotNull(lastSignalClient as ClientSignal.StartImaging)

        val s2 = ServerSignal.Status(Vector3i(1,2,3),ServerState.Imaging, listOf(22,33))
        lastSignalServer = null
        server.sendSignal(s2)
        lightSleepOn { lastSignalServer }
        val s2trans = lastSignalServer as? ServerSignal.Status
        assertNotNull(s2trans)
        assert(s2trans.state == ServerState.Imaging )
        assert(s2trans.imageSize == s2.imageSize)
        assert(s2trans.dataPorts.containsAll(s2.dataPorts))

        val s3 = ServerSignal.Status(Vector3i(1,2,3),ServerState.ShuttingDown, listOf(22,33))
        lastSignalServer = null
        server.sendSignal(s3)
        lightSleepOn { lastSignalServer }
        val s3trans = lastSignalServer as? ServerSignal.Status
        assertNotNull(s3trans)
        assert(s3trans.state == ServerState.ShuttingDown )

        server.thread.join(5000)
        assert(!server.thread.isAlive)
        client.thread.join(5000)
        assert(!client.thread.isAlive)

    }
}