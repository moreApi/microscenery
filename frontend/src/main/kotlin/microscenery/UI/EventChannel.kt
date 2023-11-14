package microscenery.UI


/**
 * c# like events.
 */
class EventChannel<T> {
    private val listeners = mutableSetOf<(T) -> Unit>()

    /**
     * Add a listener. !CAREFUL [observer] will be executed by the emitting thread. Do not Block!
     */
    operator fun plusAssign(observer: (T) -> Unit) {
        listeners.add(observer)
    }

    operator fun minusAssign(observer: (T) -> Unit) {
        listeners.remove(observer)
    }

    /**
     * Emits an event to all listeners
     */
    fun emit(value: T) {
        for (observer in listeners)
            observer(value)
    }
}