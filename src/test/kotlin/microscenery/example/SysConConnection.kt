package microscenery.example

import graphics.scenery.BoundingGrid
import graphics.scenery.Origin
import graphics.scenery.Sphere
import graphics.scenery.controls.behaviours.MouseDragPlane
import graphics.scenery.volumes.TransferFunction
import graphics.scenery.volumes.Volume
import microscenery.DefaultScene
import org.joml.Vector3f
import org.scijava.ui.behaviour.ClickBehaviour
import kotlin.concurrent.thread
import kotlin.io.path.Path

class SysConConnection : DefaultScene({ _, _ -> }) {

    val onev = Sphere(0.01f)
    val v2 = Sphere(0.01f)
    val v3 = Sphere(0.01f)

    override fun init() {
        super.init()
        cam.spatial().position = Vector3f(0f, 0f, 2f)

        //val volume = Volume.fromXML("""C:\Users\JanCasus\volumes\drosophila.xml""",hub, VolumeViewerOptions())
        val volume = Volume.fromPath(Path("""C:\Users\JanCasus\volumes\t1-head.tif"""), hub)
        volume.transferFunction = TransferFunction.ramp()
        volume.origin = Origin.FrontBottomLeft

        volume.spatial {
            scale = Vector3f(2f)
            //position = Vector3f(0f,2f,0f)
        }
        scene.addChild(volume)
        val bg = BoundingGrid()
        bg.node = volume
        scene.addChild(bg)

        val origin = Sphere(0.01f)
        scene.addChild(origin)

        val one = Sphere(0.01f)
        one.spatial {
            position = Vector3f(1f, -1f, 1f)
        }
        scene.addChild(one)


        onev.spatial {
            position = Vector3f(100f, 100f, 100f)
            scale = Vector3f(1f).div(volume.localScale())
        }
        onev.material {
            diffuse = Vector3f(1f)
        }
        volume.addChild(onev)

        v2.spatial {
            position = Vector3f(50f, 50f, 100f)
            scale = Vector3f(1f).div(volume.localScale())
        }
        volume.addChild(v2)

        v3.spatial {
            position = Vector3f(150f, 150f, 100f)
            scale = Vector3f(1f).div(volume.localScale())
        }
        volume.addChild(v3)

        thread {
            Thread.sleep(5000)
            print(scene)
        }
    }

    override fun inputSetup() {
        inputHandler?.addBehaviour(
            "planeDragObject", MouseDragPlane(
                "planeDragObject",
                { scene.findObserver() }, debugRaycast = false,
                ignoredObjects = listOf(Volume::class.java, BoundingGrid::class.java)
            )
        )
        inputHandler?.addKeyBinding("planeDragObject", "1")


        inputHandler?.addBehaviour(
            "genSysCon", behaviour = ClickBehaviour { x, y ->
                generateSysCon(listOf(onev, v2, v3).map { it.spatial().position })
            }
        )
        inputHandler?.addKeyBinding("genSysCon", "2")
    }

    fun generateSysCon(points: List<Vector3f>) {
        println("generating sys con file")
        val pathBase = "/microscenery/sysConSeqFormat/base.seq"
        val baseFile = javaClass.getResource(pathBase)
            ?: throw IllegalArgumentException("Cant find resource ${pathBase}")

        val base = baseFile.readText().lines().filter { !it.startsWith("//") }

        val pathEntity = "/microscenery/sysConSeqFormat/pointEntity.seq"
        val entityText = javaClass.getResource(pathEntity)?.readText()
            ?: throw IllegalArgumentException("Cant find resource ${pathEntity}")

        println(base.joinToString("\n"))
        points.forEachIndexed { id, point ->
            val entity = entityText.lines().map {
                when {
                    it.startsWith("Entity\$_CenterX") -> "Entity${id}_CenterX=${point.x}"
                    it.startsWith("Entity\$_CenterY") -> "Entity${id}_CenterY=${point.y}"
                    it.startsWith("Entity\$_CenterZ") -> "Entity${id}_CenterZ=${
                        point.z.toInt()
                    }" // syscon cant handle floats for z
                    else -> it.replace("$", id.toString())
                }
            }

            println("[${id}]\n" + entity.joinToString("\n") + "\n[/${id}]")
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SysConConnection().main()
        }
    }
}