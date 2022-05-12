@file:Suppress("unused")
@file:JvmName("Util")
@file:JvmMultifileClass

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy
import graphics.scenery.Settings
import graphics.scenery.serialization.Vector3fSerializer
import graphics.scenery.utils.LazyLogger
import microscenery.network.ClientSignal
import microscenery.network.ServerSignal
import org.joml.Vector3f
import org.joml.Vector3i
import org.objenesis.strategy.StdInstantiatorStrategy


val MicroscenerySettings = Settings(prefix = "microscenery.", propertiesFile = "microscenery.properties")


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
        kryo.register(Vector3f::class.java, Vector3fSerializer())
        kryo.register(Vector3i::class.java, Vector3iSerializer())
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

class Vector3iSerializer: Serializer<Vector3i>() {
    val logger by LazyLogger()
    override fun write(kryo: Kryo, output: Output, vector: Vector3i) {
        kryo.writeClassAndObject(output, intArrayOf(vector.x, vector.y, vector.z))
//        logger.info("Serialized ${vector.x}/${vector.y}/${vector.z}")
    }

    override fun read(kryo: Kryo, input: Input, oobClass: Class<out Vector3i>): Vector3i {
        val arr = kryo.readClassAndObject(input) as IntArray
        return Vector3i(arr[0], arr[1], arr[2])
    }

    init {
        // TODO: Should this be true or false?
        isImmutable = false
    }
}


