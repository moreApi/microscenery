package microscenery.example.microscope

import graphics.scenery.utils.extensions.plus
import microscenery.MicroscenerySettings
import microscenery.example.microscope.LocalMMScene.Companion.initLocalMMCore
import microscenery.hardware.micromanagerConnection.MMConnection
import microscenery.isFullyLessThan
import microscenery.toReadableString
import org.joml.Vector3f
import java.io.File
import kotlin.system.measureNanoTime

fun main() {
    //demo
    //MicroscenerySettings.set("MMConnection.core.configuration","C:/Program Files/Micro-Manager-2.0gamma/MMConfig_fake.cfg")
    // with picard stage
    MicroscenerySettings.set("MMConnection.core.configuration","C:/Program Files/Micro-Manager-2.0gamma/MMConfig_fake_picard.cfg")

    val con = MMConnection(initLocalMMCore())

    val min = 0f
    val max = 100f
    val offset = 100f
    val runs = 50
    val dist = 30f

    con.moveStage(Vector3f(offset+(max-min)/2),true)

    val results = (1..runs).map{
        if (it%(runs/10) == 0) println("$it / $runs reached")
        val start = con.stagePosition
        //val target = Random.random3DVectorFromRange(min,max) + Vector3f(offset)
        //val target = if (it%2 == 0) Vector3f(50f) else Vector3f(1f)
        var newTarget = Vector3f(-1f)
        fun newTargetIsInBounds(): Boolean{
            return Vector3f(min+offset).isFullyLessThan(newTarget) && newTarget.isFullyLessThan(Vector3f(max+offset))
        }
//        while (!newTargetIsInBounds()){
//            val dir = Random.random3DVectorFromRange(-1f,1f).normalize()
//            newTarget = start + dir * dist
//        }

        val dir = Vector3f()
        dir.y = if (it%2 == 0) 30f else -30f
        newTarget = start + dir


        val time = measureNanoTime {
            con.moveStage(newTarget,true)
        }
        DataPoint(time,start,newTarget)
    }
    File("results.csv").printWriter().use { out ->
        results.forEach {
            out.println("${it.time};${it.from.toReadableString()};${it.to.toReadableString()}")
            println("${it.time};${it.from.toReadableString()};${it.to.toReadableString()}")
        }
    }


}
data class DataPoint(val time: Long, val from:Vector3f, val to:Vector3f)