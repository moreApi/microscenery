@file:Suppress("unused")

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy
import microscenery.network.ClientSignal
import microscenery.network.ServerSignal
import org.objenesis.strategy.StdInstantiatorStrategy
import java.io.FileInputStream
import java.io.IOException
import java.util.*


fun getProperty(name:String): String? {
    try {
        FileInputStream("microscenery.properties").use { input ->
            val prop = Properties()

            // load a properties file
            prop.load(input)

            // get the property value and print it out
            return prop.getProperty(name)
        }
    } catch (ex: IOException) {
        throw Exception("Could not find property $name", ex)
    }
}

fun getPropertyString(name: String): String{
    return getProperty(name) ?: throw IllegalStateException("Could not get property $name")
}

fun getPropertyInt(name: String): Int{
    val raw = getProperty(name) ?: throw IllegalStateException("Could not get property $name")
    return (raw.toIntOrNull())?: throw IllegalStateException("Property $name cant be casted to Int")
}

fun <T,U,W>T?.let(any: U?, call: (T,U)->W): W? =
    if (this != null && any != null)
        call(this,any)
    else
        null

// why this? -> https://github.com/EsotericSoftware/kryo#pooling
val ky = object : ThreadLocal<Kryo>() {
    override fun initialValue(): Kryo {
        val kryo = Kryo()
        kryo.instantiatorStrategy = DefaultInstantiatorStrategy(StdInstantiatorStrategy())
        kryo.isRegistrationRequired = false
        kryo.references = true
        kryo.setCopyReferences(true)
        kryo.register(ServerSignal::class.java)
        kryo.register(ClientSignal::class.java)
        return kryo
    }
}
fun freeze(): Kryo {
    return ky.get()
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


