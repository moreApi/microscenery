package microscenery.network

import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo

class BonjourService(val type: String = "_xrsidecar._tcp.local.") : AutoCloseable {
    private val jmDNS: JmDNS = JmDNS.create(InetAddress.getLocalHost())
    private val logger = LoggerFactory.getLogger(BonjourService::class.java)


    fun register(name: String, port: Int, text: String) {
        try {
            // Register a service
            val serviceInfo = ServiceInfo.create(type, name, port, text)
            jmDNS.registerService(serviceInfo)
            logger.info("Registered Bonjour service at ${jmDNS.inetAddress}:$port name: $name type: $type text: $text")
        } catch(e: IOException) {
            logger.error(e.message)
        }
    }

    override fun close() {
        // Unregister all services and close
        jmDNS.unregisterAllServices()
        logger.info("Unregistered Bonjour all service at ${jmDNS.inetAddress}")
        jmDNS.close()
    }
}