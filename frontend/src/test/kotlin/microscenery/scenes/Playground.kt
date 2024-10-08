package microscenery.scenes

import graphics.scenery.Node
import graphics.scenery.RichNode
import microscenery.DefaultScene
import microscenery.nextVector3f
import microscenery.primitives.LineNode
import org.joml.Vector3f
import kotlin.concurrent.thread
import kotlin.random.Random

class Playground : DefaultScene(VR = false, width = 1024, height = 1024) {

    override fun init() {
        super.init()
        cam.spatial().position = Vector3f(0f,0f,5f)
        scene.addChild(generateTree(Random(0L), Vector3f(0f,.5f,0f), 1f, 5))

        thread{
            while (true){
                Thread.sleep(200)
                scene
            }
        }
    }

    data class TreeNode(val pos: Vector3f, val prev: TreeNode?)

    private fun generateTree(random: Random, dir: Vector3f, distance: Float, iterations: Int): Node {

        var nodes = mutableListOf(listOf(TreeNode(Vector3f(),null)))

        for (i in 0 until iterations){
            nodes += nodes[i].flatMap{ parent ->
                listOf(
                    TreeNode(((random.nextVector3f().add(parent.pos)).add(dir)).mul(distance), parent),
                    TreeNode(((random.nextVector3f().add(parent.pos)).add(dir)).mul(distance), parent)
                )
            }
        }

        var visualizedNodes = listOf(LineNode(radius = 0.1f) to nodes[0][0])

        nodes.drop(1).flatten().forEach {
            val parent = visualizedNodes.find { vn -> vn.second == it.prev } ?: return@forEach
            val lNode = LineNode( listOf(parent.first), radius = 0.1f,)
            lNode.spatial().position = it.pos
            visualizedNodes += lNode to it
        }

        return RichNode().also { rn ->
            visualizedNodes.forEach{ vn ->
                rn.addChild(vn.first)
            }
        }
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
Vector3f(0f),
1.2f, 3))
seed
 */