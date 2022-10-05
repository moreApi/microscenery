package microscenery.unit.network

import microscenery.lightSleepOnCondition
import microscenery.Agent
import org.junit.jupiter.api.Test


class AgentTest {

    @Test
    fun closing() {
        var onClosingCalled = false

        val client = object : Agent() {
            init {
                startAgent()
            }

            override fun onLoop() {
                Thread.sleep(50000)
            }

            override fun onClose() {
                onClosingCalled = true
            }
        }


        lightSleepOnCondition { client.running }
        assert(client.running)
        val t = client.close()
        t.join()
        assert(!t.isAlive)
        assert(!client.running)
        assert(onClosingCalled)
    }
}