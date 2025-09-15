package anon

import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.createType

class PropertyChangeEvent<T>(val kProperty: KProperty<*>, val old: T, val new: T)

open class PropertyChangeObservable{
    val changeEvents = EventChannel<PropertyChangeEvent<Any?>>()

    fun <T>propertyObservable(init: T): ReadWriteProperty<Any?, T> {
        return Delegates.observable(init) { kProperty: KProperty<*>, t: T, t1: T ->
            changeEvents.emit(PropertyChangeEvent(kProperty, t, t1))
        }
    }

    inline fun <reified T>registerListener(kProperty: KProperty<*>, crossinline action: (T?, T?) -> Unit) {
        // don't care about nullability but the rest of the type has to fit
        if (!(kProperty.returnType == T::class.createType(nullable = true) || kProperty.returnType == T::class.createType(nullable = false))){
            throw IllegalArgumentException("wrong type for property listener function got ${kProperty.returnType} needed ${T::class.createType()}")
        }

        changeEvents += { event ->
            if (event.kProperty == kProperty) {
                action(event.old as? T,event.new as? T)
            }
        }
    }
}