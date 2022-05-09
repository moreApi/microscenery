@file:Suppress("unused")

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy
import graphics.scenery.Settings
import microscenery.network.ClientSignal
import microscenery.network.ServerSignal
import org.objenesis.strategy.StdInstantiatorStrategy

val GlobalSettings = Settings(prefix = "microscenery.", propertiesFile = "microscenery.properties")


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


