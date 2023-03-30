package fromScenery

import org.joml.*
import java.io.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.swing.JOptionPane
import kotlin.io.path.Path
import kotlin.io.path.outputStream

/**
 * Flexible settings store for scenery. Stores a hash map of <String, Any>,
 * which one can query for a specific setting and type then.
 *
 * @author Ulrik Günther <hello@ulrik.is>
 * @author Konrad Michel <Konrad.Michel@mailbox.tu-dresden.de>
 */
class Settings(val prefix : String = "scenery.", inputPropertiesStream : InputStream? = null) {
    private var settingsStore = ConcurrentHashMap<String, Any>()
    private val logger by LazyLogger()

    var settingsUpdateRoutines : HashMap<String, ArrayList<() -> Unit>> = HashMap()

    init {
        val properties = System.getProperties()
        properties.forEach { p ->
            val key = p.key as? String ?: return@forEach
            val value = p.value as? String ?: return@forEach

            if(!key.startsWith(prefix)) {
                return@forEach
            }

            val parsed = parseType(value)
            set(key.substringAfter(prefix), parsed)
        }

        if(inputPropertiesStream != null)
        {
            loadProperties(inputPropertiesStream)
        }

    }

    /**
     * Loads the .properties [file]
     * Currently not clearing the old settings -> Overwrites the already set and add new ones. Old stay untouched, if not set by new settings
     */
    fun loadProperties(inputStream : InputStream)
    {
        val prop = Properties()
        prop.load(inputStream)
        prop.propertyNames().toList().forEach { propName ->
            set(propName as String, parseType(prop.getProperty(propName)))
        }
    }

    /**
     * Saves the currently set settings into [path] if set, or the default properties location (root) set in [this]
     */
    fun saveProperties(path : String? = null)
    {
        val props = Properties()
        this.getAllSettings().sortedDescending().forEach { setting ->
            props.setProperty(setting, this.getOrNull<String?>(setting).toString())
        }
        val out : OutputStream
        if(path != null)
            out = Path(path).outputStream()
        else
            out = Path(File("").absolutePath + "properties.properties").outputStream()

        props.store(out, null)
    }

    /**
     * Parses the type from the incoming string, returns the casted value
     */
    fun parseType(value : String): Any = when {
        value.lowercase() == "false" || value.lowercase() == "true" -> value.toBoolean()
        value.contains("(") && value.contains(")") && (value.contains(".") || value.lowercase().contains("f")) -> makeVector(value.replace("(", "").replace(")", ""))
        value.lowercase().contains(".") && value.lowercase().toFloatOrNull() != null -> value.lowercase().toFloat()
        value.lowercase().contains("f") && value.lowercase().replace("f", "").toFloatOrNull() != null -> value.lowercase().replace("f", "").toFloat()
        value.lowercase().contains(".") && value.lowercase().contains("f") && value.lowercase().replace("f", "").toFloatOrNull() != null -> value.lowercase().replace("f", "").toFloat()
        value.lowercase().contains("l") && value.lowercase().replace("l", "").toLongOrNull() != null -> value.lowercase().replace("l", "").toLong()
        value.toIntOrNull() != null -> value.toInt()
        else -> value
    }

    private fun makeVector(value : String) : Any {
        val snippets = value.trim().split(",".toRegex()).toTypedArray()

        var allInts = true
        var allFloats = true
        snippets.forEachIndexed { i, it ->
            allInts = allInts && checkType(parseType(it), listOf("Integer", "Long"))
            allFloats = allFloats && checkType(parseType(it), listOf("Float", "Double"))
            if(!(allInts || allFloats))
                throw NumberFormatException("Wrong type inserted at index $i")
        }
        return when (snippets.size) {
            2 ->
            {
                if(allInts)
                    return Vector2i(snippets[0].toInt(), snippets[1].toInt())
                else
                    return Vector2f(snippets[0].toFloat(), snippets[1].toFloat())
            }
            3 ->
            {
                if(allInts)
                    return Vector3i(snippets[0].toInt(), snippets[1].toInt(), snippets[2].toInt())
                else
                    return Vector3f(snippets[0].toFloat(), snippets[1].toFloat(), snippets[2].toFloat())
            }
            4 ->
            {
                if(allInts)
                    return Vector4i(snippets[0].toInt(), snippets[1].toInt(), snippets[2].toInt(), snippets[3].toInt())
                else
                    return Vector4f(snippets[0].toFloat(), snippets[1].toFloat(), snippets[2].toFloat(), snippets[3].toFloat())
            }
            else -> IllegalArgumentException("Too little or too many arguments!")
        }
    }

    private fun checkType(first : Any, seconds : List<String>) : Boolean {
        seconds.forEach {
            if(first::class.java.typeName == "java.lang.$it")
                return true
        }
        return false
    }

    /**
     * Query the settings store for a setting [name] and type T
     *
     * @param[name] The name of the setting
     * @return The setting as type T
     */
    fun <T> get(name: String, default: T? = null): T {
        if(!settingsStore.containsKey(name)) {
            if(default == null) {
                logger.warn("Settings don't contain '$name'")
            } else {
                logger.debug("Settings don't contain '$name'")
            }
        }

        @Suppress("UNCHECKED_CAST")
        val s = settingsStore[name] as? T
        return s
            ?: (default ?: throw IllegalStateException("Cast of $name failed, the setting might not exist (current value: $s)"))
    }

    /**
     * Query the settings store for a setting [name] and type T. If it can not be found or cast to T null is returned.
     *
     * @param[name] The name of the setting
     * @return The setting as type T
     */
    fun <T> getOrNull(name: String): T? {
        if(!settingsStore.containsKey(name)) {
            logger.debug("Settings don't contain '$name'")
        }
        @Suppress("UNCHECKED_CAST")
        return settingsStore[name] as? T
    }

    /**
     * Compatibility function for Java, see [get]. Returns the settings value for [name], if found.
     */
    @JvmOverloads fun <T> getProperty(name: String, default: T? = null): T{
        if(!settingsStore.containsKey(name)) {
            if(default == null) {
                logger.warn("Settings don't contain '$name'")
            } else {
                logger.debug("Settings don't contain '$name'")
            }
        }

        @Suppress("UNCHECKED_CAST")
        val s = settingsStore[name] as? T
        return s
            ?: (default ?: throw IllegalStateException("Cast of $name failed, the setting might not exist (current value: $s)"))
    }

    /**
     * Calls a function, if set, from [settingsUpdateRoutines], for the given [setting]
     * @param[setting] Name of the setting
     */
    private fun onValueChange(setting : String) {
        settingsUpdateRoutines[setting]?.forEach { it.invoke() }
    }

    /**
     * Add or a setting in the store only if it does not exist yet.
     * Will only allow replacement if types of existing and new setting match.
     *
     * @param[name] Name of the setting.
     * @param[contents] Contents of the setting, can be anything.
     */
    fun setIfUnset(name: String, contents: Any): Any {
        return settingsStore[name] ?: set(name, contents)
    }

    /**
     * Add or replace a setting in the store. Will only allow replacement
     * if types of existing and new setting match.
     *
     * @param[name] Name of the setting.
     * @param[contents] Contents of the setting, can be anything.
     */
    fun set(name: String, contents: Any): Any {
        // protect against unintended type change
        var current = settingsStore[name]

        if (current != null) {
            if(current == contents)
                return current

            val type: Class<*> = current.javaClass

            if (type != contents.javaClass) {
                logger.warn("Casting $name from ${type.simpleName} to ${contents.javaClass.simpleName}. Are you sure about this?")
            }

            when {
                type == contents.javaClass -> settingsStore[name] = contents
                current is Float && contents is Double -> settingsStore[name] = contents.toFloat()
                current is Int && contents is Float -> settingsStore[name] = contents.toInt()
                current is Int && contents is Double -> settingsStore[name] = contents.toInt()
                else -> {
                    logger.warn("Will not cast $contents from ${contents.javaClass} to $type, $name will stay ${settingsStore[name]}")
                    current = null
                }
            }
        } else {
            settingsStore[name] = contents
        }
        onValueChange(name)

        return current ?: contents
    }

    /**
     * Adds an update routine lambda to a specific setting [setting], which is called when the setting changes inside the [settingsStore]
     */
    fun addUpdateRoutine(setting : String, update: () -> Unit) {
        if(!settingsUpdateRoutines.containsKey(setting)) {
            settingsUpdateRoutines[setting] = arrayListOf(update)
        } else {
            settingsUpdateRoutines[setting]!! += update
        }
    }

    /**
     * Lists all settings currently stored as String.
     */
    fun list(): String {
        return settingsStore.map { "${it.key}=${it.value} (${it.value.javaClass.simpleName})" }.sorted().joinToString("\n")
    }

    /**
     * Return the names of all settings as a [List] of Strings.
     */
    fun getAllSettings(): List<String> {
        return settingsStore.keys().toList()
    }
}
