@file:Suppress("unused")

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

