package microscenery.hardware

import fromScenery.lazyLogger

/**
 * used by the imported hardware stuff from ?ClearControl? which is java
 */
class ReportingUtils {

    companion object {
        /** Logger for this application, will be instantiated upon first use. */
        private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

        @JvmStatic
        fun logError(t: Throwable, msg: String) {
            logger.error(msg, t)
        }

        @JvmStatic
        fun logError(t: Throwable) {
            logger.error("error", t)
        }
    }
}