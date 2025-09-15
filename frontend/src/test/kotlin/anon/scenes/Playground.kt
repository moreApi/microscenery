package anon.scenes

import fromScenery.utils.extensions.minus
import fromScenery.utils.extensions.plus
import fromScenery.utils.extensions.times
import graphics.scenery.Node
import graphics.scenery.RichNode
import anon.DefaultScene
import anon.nextVector3f
import anon.primitives.LineNode
import org.joml.Vector3f
import kotlin.concurrent.thread
import kotlin.random.Random

class Playground : DefaultScene(VR = false, width = 1024, height = 1024) {

    override fun init() {
        super.init()
        cam.spatial().position = Vector3f(0f,2.5f,7f)

        val seed = 3824716//Random.nextInt()
        scene.addChild(generateTree(
            Random(seed),
            Vector3f(0f,0.5f,0f),
            2f,
            3,
            1..3))

        thread{
            while (true){
                Thread.sleep(200)
                scene
            }
        }
    }

    data class TreeNode(val pos: Vector3f, val prev: TreeNode?, var visualisation: LineNode? = null)

    private fun generateTree(random: Random, dir: Vector3f, stepSize: Float, iterations: Int, childrenPerIteration: IntRange)
        : Node {

        val nodes = mutableListOf(listOf(TreeNode(Vector3f(),null)))

        fun nextPos(parent: Vector3f): Vector3f {
            return ((random.nextVector3f() - Vector3f(0.5f)) * stepSize
                    + dir * stepSize
                    + parent)
        }

        for (i in 0 until iterations){
            nodes += nodes[i].flatMap{ parent ->
                (1 .. random.nextInt(childrenPerIteration.first, childrenPerIteration.endInclusive+1))
                    .map{
                        TreeNode(nextPos(parent.pos), parent)
                    }
            }
        }

        // -- visualisation --
        val root = RichNode()
        nodes.flatten().forEach { treeNode ->
            val parent = treeNode.prev?.visualisation?.let { listOf(it) } ?: emptyList()
            val lNode = LineNode( parent, radius = 0.1f,)
            lNode.spatial().position = treeNode.pos
            treeNode.visualisation = lNode
            root.addChild(lNode)
        }

        return root
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Playground().main()
        }
    }
}


/*
scene.children[3]?.detach()
val seed = Random.nextInt()
scene.addChild(generateTree(
    Random(seed),
    Vector3f(0f,0.5f,0f),
    2f,
    3,
    1..3))
seed
 */