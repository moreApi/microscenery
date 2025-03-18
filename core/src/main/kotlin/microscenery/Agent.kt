package microscenery

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlin.concurrent.thread


/**
 * Agent class to handle its thread and shutting down.
 *
 * !! ATTENTION!! Call [startAgent] eg. in the Init method of your class
 */
abstract class Agent(val isDaemon: Boolean = true) {

    var running = false
        private set
    private lateinit var thread: Thread

    /**
     * Starts this agent. Blocks until the thread is running.
     */
    fun startAgent() {

        runBlocking {
            // this is to make sure that the thread is started and running after initialisation. Makes testing easier.
            val lock = Semaphore(1, 1)

            thread = thread(isDaemon = isDaemon, name = "Agent: ${this@Agent::class.simpleName}") {
                running = true
                lock.release()
                try {
                    onStart()
                    while (running && !Thread.currentThread().isInterrupted) {
                        onLoop()
                    }
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
                onClose()
            }

            lock.acquire()
        }
    }

    /**
     * Called in a while loop once started. Add [Thread.sleep] if needed. Otherwise, the agent will busy wait.
     */
    protected abstract fun onLoop()

    /**
     * Called before the first loop.
     */
    protected open fun onStart() {}


    /**
     * Called after the last loop or an interrupt.
     */
    protected open fun onClose() {}

    /**
     * Interrupts the thread and stops additional loops.
     *
     * @return a thread object to join on
     */
    fun close(): Thread {
        running = false
        thread.interrupt()
        return thread
    }
}