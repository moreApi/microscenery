package microscenery.scenes.stageStudy

import graphics.scenery.Camera
import graphics.scenery.RichNode
import graphics.scenery.attribute.spatial.Spatial
import microscenery.Agent
import microscenery.MicrosceneryHub
import microscenery.UI.UIModel
import microscenery.stageSpace.StageSpaceManager
import org.joml.Quaternionf
import org.joml.Vector3f
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

class StudySpatialLogger(val camera: Camera, val msHub: MicrosceneryHub, file: File?, val loggingInterval: Long = 200) :
    Agent() {
    val uiModel = msHub.getAttribute(UIModel::class.java)
    val stageSpaceManager = msHub.getAttribute(StageSpaceManager::class.java)

    // vr spatial are weird! But their children are alright
    val cameraSpatialBobbel = RichNode()
    val leftControllerSpatialBobbel = RichNode()
    val rightControllerSpatialBobbel = RichNode()

    val writer: BufferedWriter

    private val otherEvents: BlockingQueue<LoggingEvent> = ArrayBlockingQueue(50)

    init {
        val sdf = SimpleDateFormat("yyyy-MM-dd--hh-mm-ss")
        val currentDate = sdf.format(Date())
        val f = file ?: File("spatialLogs/$currentDate.csv")

        writer = BufferedWriter(FileWriter(f))

        writer.write("time;name;PosX;PosY;PosZ;RotX;RotY;RotZ;RotW")
        writer.newLine()

        camera.addChild(cameraSpatialBobbel)

        startAgent()
    }

    fun logEvent(name: String, extraParams: List<String>? = emptyList(), time: Long = System.currentTimeMillis()) {
        otherEvents.put(LoggingEvent(name, extraParams, time))
    }

    override fun onLoop() {
        writeOutOtherEvents()
        writeSpatialLog("camera", cameraSpatialBobbel.spatial())
        writer.newLine()
        writeSpatialLog("focusTarget", stageSpaceManager.focusManager.focusTarget.spatial())
        writer.newLine()
        writeVectorLog("scale",stageSpaceManager.scaleAndRotationPivot.spatial().scale)
        uiModel.leftVRController?.model?.let {
            if (leftControllerSpatialBobbel.parent == null) it.addChild(leftControllerSpatialBobbel)
            writeSpatialLog("leftHand", leftControllerSpatialBobbel.spatial())
            writer.newLine()
        }
        uiModel.rightVRController?.model?.let {
            if (rightControllerSpatialBobbel.parent == null) it.addChild(rightControllerSpatialBobbel)
            writeSpatialLog("rightHand", rightControllerSpatialBobbel.spatial())
            writer.newLine()
        }
        writer.newLine()
        writer.flush()
        Thread.sleep(loggingInterval)
    }


    private fun writeOutOtherEvents() {
        while (otherEvents.isNotEmpty()) {
            val e = otherEvents.poll()
            writer.write(e.time.toString() + ";" + e.name + e.extraParams?.let { ";" + it.joinToString(";") })
            writer.newLine()
        }
        writer.flush()
    }


    private fun writeVectorLog(name: String, vec: Vector3f) {
        val logLine = System.currentTimeMillis().toString() + ";$name;" + vec.toCSVString()
        writer.write(logLine)
    }

    private fun writeSpatialLog(name: String, spatial: Spatial) {
        val logLine = System.currentTimeMillis().toString() + ";$name;" + spatial.toCSVString()
        writer.write(logLine)
    }

    override fun onClose() {
        writeOutOtherEvents()
        writer.close()
    }

    data class LoggingEvent(
        val name: String,
        val extraParams: List<String>? = null,
        val time: Long = System.currentTimeMillis()
    )

    companion object {
        fun Spatial.toCSVString(): String {
            return worldPosition().toCSVString() + ";" + worldRotation().toCSVString()
        }

        fun Vector3f.toCSVString(): String {
            return "%.4f".format(x) + ";" + "%.4f".format(y) + ";" + "%.4f".format(z)
        }

        fun Quaternionf.toCSVString(): String {
            return "%.4f".format(x) + ";" + "%.4f".format(y) + ";" + "%.4f".format(z) + ";" + "%.4f".format(w)
        }
    }
}
