package microscenery.hardware

import graphics.scenery.utils.LazyLogger
import kotlin.math.log

class ReportingUtils {

    companion object {
        /** Logger for this application, will be instantiated upon first use. */
        private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))
        @JvmStatic
        fun logError(t: Throwable, msg: String){
            logger.error(msg,t)
        }

        @JvmStatic
        fun logError(t: Throwable){
            logger.error("error",t)
        }
    }
}