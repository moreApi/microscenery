package microscenery.example.microscope

import microscenery.MicroscenerySettings
import microscenery.hardware.micromanagerConnection.MMConnection
import microscenery.toReadableString
import org.joml.Vector3f
import java.io.File
import kotlin.system.measureNanoTime

fun main() {
    //demo
    MicroscenerySettings.set("MMConnection.core.configuration","C:/Program Files/Micro-Manager-2.0gamma/MMConfig_fake.cfg")
    // with picard stage
    //MicroscenerySettings.set("MMConnection.core.configuration","C:/Program Files/Micro-Manager-2.0gamma/MMConfig_fake_picard.cfg")

    val con = MMConnection()

    val min = 0f
    val max = 100f
    val offset = 0f
    val runs = 50

    val results = (1..5000).map{
        if (it%500 == 0) println("$it reached")
        val start = con.stagePosition
        //val target = Random.random3DVectorFromRange(min,max) + Vector3f(offset)
        val target = if (it%2 == 0) Vector3f(50f) else Vector3f(1f)
        val time = measureNanoTime {
            con.moveStage(target,true)
        }
        DataPoint(time,start,target)
    }
    File("results.csv").printWriter().use { out ->
        results.forEach {
            out.println("${it.time};${it.from.toReadableString()};${it.to.toReadableString()}")
            //println("${it.time};${it.from.toReadableString()};${it.to.toReadableString()}")
        }
    }


}
data class DataPoint(val time: Long, val from:Vector3f, val to:Vector3f)