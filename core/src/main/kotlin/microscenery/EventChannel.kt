package microscenery

import java.util.concurrent.ConcurrentHashMap

/**
 * c# like events.
 */
class EventChannel<T> {
    private val listeners = ConcurrentHashMap<(T) ->Unit,(T) ->Unit>() // there is no concurrent set so we use the keys of a map...

    /**
     * Add a listener. !CAREFUL [observer] will be executed by the emitting thread. Do not Block!
     */
    operator fun plusAssign(observer: (T) -> Unit) {
        listeners[observer] = observer
    }

    operator fun minusAssign(observer: (T) -> Unit) {
        listeners.remove(observer)
    }

    /**
     * Emits an event to all listeners
     */
    fun emit(value: T) {
        for (observer in listeners)
            observer.value(value)
    }
}