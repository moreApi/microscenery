package microscenery.simulation

import fromScenery.utils.extensions.minus
import fromScenery.utils.extensions.plus
import fromScenery.utils.extensions.times
import graphics.scenery.*
import graphics.scenery.attribute.material.Material
import graphics.scenery.attribute.spatial.HasSpatial
import graphics.scenery.primitives.Cylinder
import microscenery.*
import microscenery.Settings.StageSpace.HideStageSpaceLabel
import microscenery.primitives.LineNode
import microscenery.stageSpace.FocusManager
import microscenery.stageSpace.MicroscopeLayout
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector2i
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class StageSimulation(val stageSpaceSize: Float = 1000f, val imageSize: Int = 100, val random: Random) {

    fun setupStage(msHub: MicrosceneryHub, scene: Scene): StageSpaceManager {
        MicroscenerySettings.set(HideStageSpaceLabel, true)

        val hw = SimulationMicroscopeHardware(
            msHub,
            stageSize = Vector3f(stageSpaceSize),
            imageSize = Vector2i(imageSize),
            maxIntensity = 4000
        )
        val stageSpaceManager = StageSpaceManager(
            hw,
            scene,
            msHub,
            layout = MicroscopeLayout.Default(MicroscopeLayout.Axis.Z)
        )

        stageSpaceManager.sliceManager.transferFunctionManager.apply {
            maxDisplayRange = 4100f
            minDisplayRange = 0f
        }

        stageSpaceManager.focusManager.mode = FocusManager.Mode.STEERING
        stageSpaceManager.goLive()

        return stageSpaceManager
    }

    fun tube(stageRoot: HasSpatial, position: Vector3f, radius: Float = 200f, height: Float = 400f): List<Vector3f> {
        Cylinder(radius * 0.95f, height, 16).let { cy ->
            CylinderSimulatable.addTo(cy,stageRoot.spatial()).maxIntensity = 3000
            cy.hideMaterial()
            cy.spatial().position = position + Vector3f(0f, -height * 0.5f, 0f)
            stageRoot.addChild(cy)
        }

        Cylinder(radius, height, 16).let { cy ->
            CylinderSimulatable.addTo(cy,stageRoot.spatial()).maxIntensity = 3000
            cy.hideMaterial()
            cy.spatial().position = position + Vector3f(0f, -height * 0.5f, 0f)
            stageRoot.addChild(cy)
        }

        val targetPositions = mutableListOf<Vector3f>()
        while (targetPositions.size < 10) {
            val a = random.nextFloat()
            val new = Vector3f(
                radius * 0.7f * cos(a * 2f * Math.PI.toFloat()),
                (random.nextFloat() - 0.5f) * height,
                radius * 0.7f * sin(a * 2f * Math.PI.toFloat())
            ) + position

            val targetSize = 30f
            if (targetPositions.any { (it - new).length() < targetSize * 2 })
                continue
            else {
                targetPositions.add(new)
            }
        }
        return targetPositions
    }

    fun tubeScenario(stageRoot: HasSpatial, radius: Float = 200f, roiHeight: Float = 600f): List<Vector3f> {
        val roiPos = random.nextVector3f()
        roiPos.y = roiPos.y * (stageSpaceSize - roiHeight) + roiHeight / 2
        roiPos.x = roiPos.x * stageSpaceSize * 0.2f + stageSpaceSize / 2
        roiPos.z = roiPos.z * stageSpaceSize * 0.2f + stageSpaceSize / 2

        val paddedStageSpaceSize = stageSpaceSize * 1.2f //to include padding because of estimated image size
        if (roiPos.y + roiHeight / 2 < paddedStageSpaceSize) {
            // add extension tube from the top
            val pos = roiPos.copy()
            pos.y = (paddedStageSpaceSize + roiPos.y + roiHeight / 2f) / 2
            val height = (paddedStageSpaceSize - pos.y) * 2
            tube(stageRoot, pos, radius, height)
        }

        return tube(stageRoot, roiPos, radius, roiHeight)
    }

    data class TreeNode(val pos: Vector3f, val prev: TreeNode?, var visualisation: LineNode? = null)

    private fun Node.generateTree(
        random: Random,
        dir: Vector3f,
        stepSize: Float,
        iterations: Int,
        childrenPerIteration: IntRange,
        stageRoot: HasSpatial
    ): MutableList<List<TreeNode>> {

        val nodes = mutableListOf(listOf(TreeNode(Vector3f(), null)))

        fun nextPos(parent: Vector3f): Vector3f {
            return ((random.nextVector3f() - Vector3f(0.5f)) * stepSize
                    + dir * stepSize
                    + parent)
        }

        for (i in 0 until iterations) {
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
            parent.firstOrNull()?.let { lNode.connectTo(it)}
            lNode.lines.forEach {
                CylinderSimulatable.addTo(it,stageRoot.spatial()).maxIntensity = 3000
                it.hideMaterial()
            }
            lNode.hideMaterial()
        }

        return nodes
    }

    fun axionScenario(
        stageRoot: HasSpatial, random: Random = Random(3824716),
        dir: Vector3f = Vector3f(0f, -0.5f, 0f),
        stepSize: Float = 300f,
        iterations: Int = 3,
        childrenPerIteration: IntRange = 1..3
    ): List<Vector3f> {
        val root = RichNode()
        root.spatial().position = Vector3f(stageSpaceSize / 2, stageSpaceSize, stageSpaceSize / 2)
        val treeNodes = root.generateTree(random, dir, stepSize, iterations, childrenPerIteration, stageRoot = stageRoot)
        stageRoot.addChild(root)

        return treeNodes.last().map { it.pos + root.spatial().position }
    }

    fun scaffold(stageRoot: Node): List<Vector3f> {
        // size to pos
        val boxes = listOf(
            Vector3f(10f, 500f, 10f) to Vector3f(0f, 0f, 0f),
            Vector3f(500f, 30f, 10f) to Vector3f(0f, 0f, 0f),
            Vector3f(30f, 30f, 300f) to Vector3f(0f, 0f, 0f),
            Vector3f(30f, 20f, 200f) to Vector3f(250f, 0f, 0f),
            Vector3f(30f, 20f, 200f) to Vector3f(-250f, 0f, 0f),
            Vector3f(300f, 20f, 20f) to Vector3f(0f, 250f, 0f),
            Vector3f(30f, 20f, 200f) to Vector3f(0f, -250f, 0f),
        )

        boxes.forEach {
            val box = Box(it.first)
            box.spatial().position = it.second

            box.material().cullingMode = Material.CullingMode.FrontAndBack
            BoxSimulatable.addTo(box).also {
                it.range = 50f
                it.maxIntensity = 4000
            }
            stageRoot.addChild(box)
        }

        return boxes.drop(3).flatMap {
            listOf(it.first * 0.5f + it.second, it.first * -0.5f + it.second)
        }
    }

    fun lightBulb(stageRoot: Node) {

        val box = Box(Vector3f(5f, 10f, 5f))
        box.material().cullingMode = Material.CullingMode.FrontAndBack
        BoxSimulatable.addTo(box).also {
            it.range = 5f
            it.maxIntensity = 4000
        }
        stageRoot.addChild(box)

        val sphere = Sphere(10f)
        sphere.material().cullingMode = Material.CullingMode.FrontAndBack
        sphere.spatial().position.y = 15f
        SphereSimulatable.addTo(sphere)
        stageRoot.addChild(sphere)
    }

    companion object {

        fun Scene.toggleMaterialRendering() {
            this.discover { node ->
                node.ifHasAttribute(Simulatable::class.java) {
                    node.ifMaterial {
                        if (this.cullingMode == Material.CullingMode.FrontAndBack)
                            node.showMaterial()
                        else
                            node.hideMaterial()
                    }
                }
                false
            }
        }

        fun Node.showMaterial() {
            ifMaterial { cullingMode = Material.CullingMode.None }
        }

        fun Node.hideMaterial() {
            ifMaterial { cullingMode = Material.CullingMode.FrontAndBack }
        }
    }
}