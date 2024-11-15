package microscenery.simulation

import fromScenery.utils.extensions.minus
import fromScenery.utils.extensions.plus
import fromScenery.utils.extensions.times
import graphics.scenery.Node
import graphics.scenery.RichNode
import graphics.scenery.attribute.spatial.HasSpatial
import graphics.scenery.volumes.Colormap
import microscenery.nextVector3f
import microscenery.primitives.LineNode
import microscenery.simulation.StageSimulation.Companion.hideMaterial
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector3f
import java.io.File
import kotlin.random.Random

data class AxionScenario(
    val randomSeed: Long = 3824716,
    val dir: Vector3f = Vector3f(0f, -0.5f, 0f),
    val stepSize: Float = 350f,
    val iterations: Int = 3,
    val childrenPerIteration: IntRange = 1..3
) : StageSimulation.Scenario {

    override val name = "follow structure"

    override fun generate(stageSpaceManager: StageSpaceManager, stageSpaceSize: Float): List<Vector3f> {
        val random = Random(randomSeed)
        val stageRoot = stageSpaceManager.stageRoot

        stageSpaceManager.sliceManager.transferFunctionManager.loadTransferFunctionFromFile(
            File("""./frontend/src/test/kotlin/microscenery/scenes/stageStudy/axionTransferFunction""")
        )
        stageSpaceManager.sliceManager.transferFunctionManager.colormap =
            Colormap.fromPNGFile(File("""./frontend/src/test/kotlin/microscenery/scenes/stageStudy/axionColormap.png"""))


        val root = RichNode()
        root.spatial().position = Vector3f(stageSpaceSize / 2, stageSpaceSize, stageSpaceSize / 2)
        val treeNodes =
            root.generateTree(random, dir, stepSize, iterations, childrenPerIteration, stageRoot = stageRoot)
        stageRoot.addChild(root)

        stageSpaceManager.focusManager.focusTarget.spatial().position = Vector3f(stageSpaceSize / 2, stageSpaceSize, stageSpaceSize / 2)

        return treeNodes.last().map { it.pos + root.spatial().position }
    }


    private fun Node.generateTree(
        random: Random,
        dir: Vector3f,
        stepSize: Float,
        iterations: Int,
        childrenPerIteration: IntRange,
        stageRoot: HasSpatial
    ): MutableList<List<TreeNode>> {

        val first = TreeNode(Vector3f(dir) * -stepSize, null)
        val nodes = mutableListOf(listOf(first), listOf(TreeNode(Vector3f(), first)))

        fun nextPos(parent: Vector3f): Vector3f {
            return ((random.nextVector3f() - Vector3f(0.5f)) * stepSize
                    + dir * stepSize
                    + parent)
        }

        for (i in 1 until iterations + 1) {
            nodes += nodes[i].flatMap { parent ->
                (1..random.nextInt(childrenPerIteration.first, childrenPerIteration.last + 1))
                    .map {
                        TreeNode(nextPos(parent.pos), parent)
                    }
            }
        }

        // -- visualisation --
        nodes.flatten().forEach { treeNode ->
            val parent = treeNode.prev?.visualisation?.let { listOf(it) } ?: emptyList()
            val lNode = LineNode(radius = 20f, fixedConnections = true)
            lNode.spatial().position = treeNode.pos
            treeNode.visualisation = lNode
            this.addChild(lNode)
            parent.firstOrNull()?.let { lNode.connectTo(it) }
            lNode.lines.forEach {
                CylinderSimulatable.addTo(it, stageRoot.spatial()).maxIntensity = 3000
                it.hideMaterial()
            }
            lNode.hideMaterial()
        }

        return nodes
    }

    data class TreeNode(val pos: Vector3f, val prev: TreeNode?, var visualisation: LineNode? = null)
}