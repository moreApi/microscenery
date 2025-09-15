package network

import anon.lightSleepOnNull
import anon.network.*
import anon.signals.BaseClientSignal
import anon.signals.BaseServerSignal
import anon.signals.ImageMeta
import anon.signals.Slice
import org.joml.Vector3f
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.zeromq.ZContext
import java.nio.ByteBuffer
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RequestSliceTest {

    var ctx = ZContext()
    var lastClientBaseSignal: BaseClientSignal? = null
    var lastServerBaseSignal: BaseServerSignal? = null
    var lastSliceRequestSignal: BaseServerSignal? = null

    var server: ControlSignalsServer = ControlSignalsServer(ctx, 11543, "*", listOf {
        lastClientBaseSignal = it
    })
    var storage = SliceStorage()
    var biggishDataServer = BiggishDataServer(11544, "*", storage,ctx)

    var client: ControlSignalsClient = ControlSignalsClient(ctx, 11543, "*", listOf {
        lastServerBaseSignal = it
    })
    val sliceRequester = SliceRequester(client, listOf{ lastSliceRequestSignal = it})

    @AfterEach
    fun reset() {
        val agents = listOf(server,client,sliceRequester, biggishDataServer)
        agents.map { it.close() }.forEach { it.join() }

        ctx.linger = 0
        ctx.destroy()
        ctx = ZContext()
    }

    @Test
    fun announceAndRequestSlice() {
        lightSleepOnNull { lastClientBaseSignal }
        assertNotNull(lastClientBaseSignal as? BaseClientSignal.ClientSignOn)

        storage.addSlice(1, ByteBuffer.wrap(ByteArray(100) { it.toByte() }))
        server.sendSignal(Slice(1,System.currentTimeMillis(), Vector3f(),100,null, ImageMeta.EMPTY,null))

        lightSleepOnNull { lastServerBaseSignal as? Slice }
        //assert(lastSliceRequestSignal !is Slice) //it should be held back

        lightSleepOnNull { lastSliceRequestSignal as? Slice }
        val slice = lastSliceRequestSignal as Slice
        for (i in 0 until 100){
            assertEquals(i.toByte(), slice.data?.get())
        }
    }
}