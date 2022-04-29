package microscenery.unit.network

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



    }

    /**
     * sleep but continue once a value is there. Should speed up tests
     */
    fun lightSleepOn(mills: Int = 1000, target: () -> Any? ){
        for (t in 1..10){
            if (target() == null)
                Thread.sleep(mills/10L)
        }
    }

}