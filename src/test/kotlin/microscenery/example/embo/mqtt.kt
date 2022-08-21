/*
package microscenery.example.luxendo
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence


object MqttPublishSample {
    @JvmStatic
    fun main(args: Array<String>) {
        val topic = "MQTT Examples"
        val content = "Message from MqttPublishSample"
        val qos = 2
        //val broker = "tcp://mqtt.eclipseprojects.io:1883"
        val broker = "tcp://localhost:1883"
        val clientId = "JavaSample"
        val persistence = MemoryPersistence()
        try {
            val sampleClient = MqttClient(broker, clientId, persistence)
            val connOpts = MqttConnectOptions()
            connOpts.setCleanSession(true)
            println("Connecting to broker: $broker")
            sampleClient.connect(connOpts)
            println("Connected")
            sampleClient.subscribe(topic) { _, msg ->
                println("got msg: $msg")
            }
            println("Publishing message: $content")
            val message = MqttMessage(content.toByteArray())
            message.setQos(qos)
            sampleClient.publish(topic, message)
            println("Message published")
            sampleClient.disconnect()
            println("Disconnected")
            System.exit(0)
        } catch (me: MqttException) {
            System.out.println("reason " + me.getReasonCode())
            System.out.println("msg " + me.message)
            System.out.println("loc " + me.getLocalizedMessage())
            System.out.println("cause " + me.cause)
            println("excep $me")
            me.printStackTrace()
        }
    }
}*/
