package microscenery.scenes.stageStudy

import graphics.scenery.Camera
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

class StudySpatialLogger(val camera: Camera, val msHub: MicrosceneryHub, file: File?, val loggingInterval: Long = 200): Agent() {
    val uiModel = msHub.getAttribute(UIModel::class.java)
    val stageSpaceManager = msHub.getAttribute(StageSpaceManager::class.java)

    val writer: BufferedWriter

    init {
        val sdf = SimpleDateFormat("yyyy-MM-dd--hh-mm-ss")
        val currentDate = sdf.format(Date())
        val f = file ?: File("$currentDate.csv")

        writer = BufferedWriter(FileWriter(f))

        writer.write("time;name;PosX;PosY;PosZ;RotX;RotY;RotZ;RotW")
        writer.newLine()

        startAgent()
    }

    companion object{
        fun Spatial.toCSVString():String{
            return worldPosition().toCSVString() + ";" + worldRotation().toCSVString()
        }
        fun Vector3f.toCSVString():String{
            return "%.4f".format(x) + ";" + "%.4f".format(y) + ";" + "%.4f".format(z)
        }
        fun Quaternionf.toCSVString():String{
            return "%.4f".format(x) + ";" + "%.4f".format(y) + ";" + "%.4f".format(z) + ";" + "%.4f".format(w)
        }
    }

    override fun onLoop() {
        writeLog("camera", camera.spatial())
        writer.newLine()
        writeLog("focusTarget", stageSpaceManager.focusManager.focusTarget.spatial())
        writer.newLine()
        uiModel.leftVRController?.model?.spatialOrNull()?.let { writeLog("leftHand",it)
            writer.newLine() }
        uiModel.rightVRController?.model?.spatialOrNull()?.let { writeLog("rightHand",it)
            writer.newLine() }
        writer.newLine()
        writer.flush()
        Thread.sleep(loggingInterval)
    }

    private fun writeLog(name: String, spatial: Spatial) {
        val logLine = System.currentTimeMillis().toString() + ";$name;" + spatial.toCSVString()
        writer.write(logLine)
    }

    override fun onClose() {
        writer.close()
    }
}