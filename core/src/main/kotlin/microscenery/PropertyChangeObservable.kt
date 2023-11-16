package microscenery

import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class PropertyChangeEvent<T>(val kProperty: KProperty<*>, val old: T, val new: T)

open class PropertyChangeObservable{
    val changeEvents = EventChannel<PropertyChangeEvent<Any>>()

    fun <T : Any>propertyObservable(init: T): ReadWriteProperty<Any?, T> {
        return Delegates.observable(init) { kProperty: KProperty<*>, t: T, t1: T ->
            changeEvents.emit(PropertyChangeEvent(kProperty, t, t1))
        }
    }
}