package microscenery.network

import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener


private class SampleListener : ServiceListener {
    override fun serviceAdded(event: ServiceEvent) {
        println("Service added: " + event.info)
    }

    override fun serviceRemoved(event: ServiceEvent) {
        println("Service removed: " + event.info)
    }

    override fun serviceResolved(event: ServiceEvent) {
        println("Service resolved: " + event.info)
    }
}

fun main() {
    try {
        // Create a JmDNS instance
        val jmdns = JmDNS.create(InetAddress.getLocalHost())

        // Add a service listener
        jmdns.addServiceListener("_xrsidecar._tcp.local.", SampleListener())

        // Wait a bit
        Thread.sleep(30000)
    } catch (e: UnknownHostException) {
        println(e.message)
    } catch (e: IOException) {
        println(e.message)
    }
}
