package microscenery.example.network

import microscenery.MMVolumeSender
import org.zeromq.ZContext
import java.awt.Rectangle
import kotlin.concurrent.thread

val zContext = ZContext()

class MMVolumeSenderScene{

    val m = MMVolumeSender()


    init {
        //io thread
        thread {
            while (m.running){
                val input = readLine()?.trim() ?: continue
                println("got $input")

                when {
                    input == "q" -> {
                        m.stop()
                    }
                    input.startsWith("roi") -> {
                        val v = input.substringAfter("roi").trim().split(",").map { it.toInt() }.toList()
                        val r = Rectangle(v[0],v[1],v[2],v[3])
                        println("Setting ROI to $r")
                        m.mmConnection.setRoi(r)
                    }
                }
            }
        }


    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            MMVolumeSenderScene()
        }
    }
}